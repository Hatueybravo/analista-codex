package com.analista.fileorganizer.service.telegram

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.analista.fileorganizer.data.model.TelegramConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "telegram_config")

@Singleton
class TelegramBot @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val BASE_URL = "https://api.telegram.org/"
        private val KEY_BOT_TOKEN = stringPreferencesKey("bot_token")
        private val KEY_CHAT_ID = stringPreferencesKey("chat_id")
        private val KEY_ENABLED = booleanPreferencesKey("enabled")
        private val KEY_LAST_UPDATE_ID = longPreferencesKey("last_update_id")
    }

    private val api: TelegramApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TelegramApi::class.java)

    private var config = TelegramConfig()
    private var lastUpdateId: Long = 0

    val configFlow: Flow<TelegramConfig> = context.dataStore.data.map { prefs ->
        TelegramConfig(
            botToken = prefs[KEY_BOT_TOKEN] ?: "",
            chatId = prefs[KEY_CHAT_ID] ?: "",
            isEnabled = prefs[KEY_ENABLED] ?: false
        ).also { config = it }
    }

    suspend fun saveConfig(botToken: String, chatId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BOT_TOKEN] = botToken
            prefs[KEY_CHAT_ID] = chatId
            prefs[KEY_ENABLED] = true
        }
        config = TelegramConfig(botToken, chatId, true)
    }

    suspend fun disconnect() {
        context.dataStore.edit { prefs ->
            prefs[KEY_ENABLED] = false
        }
        config = config.copy(isEnabled = false)
    }

    fun isConnected(): Boolean = config.isEnabled && config.botToken.isNotEmpty() && config.chatId.isNotEmpty()

    /**
     * Verify the bot token by calling getMe.
     */
    suspend fun verifyToken(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMe(config.botToken)
            if (response.ok && response.result != null) {
                Result.success("Bot: @${response.result.username ?: response.result.firstName}")
            } else {
                Result.failure(Exception("Invalid bot token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send a text message to the configured chat.
     */
    suspend fun sendMessage(text: String): String = withContext(Dispatchers.IO) {
        if (!isConnected()) return@withContext "Telegram not configured. Set bot token and chat ID first."

        try {
            val response = api.sendMessage(
                config.botToken,
                SendMessageBody(chatId = config.chatId, text = text)
            )
            if (response.ok) {
                "Message sent to Telegram."
            } else {
                "Failed to send message to Telegram."
            }
        } catch (e: Exception) {
            "Telegram error: ${e.message}"
        }
    }

    /**
     * Send a document/file to the configured chat.
     */
    suspend fun sendDocument(file: File, caption: String? = null): String = withContext(Dispatchers.IO) {
        if (!isConnected()) return@withContext "Telegram not configured."

        try {
            val requestFile = file.asRequestBody("application/octet-stream".toMediaType())
            val filePart = MultipartBody.Part.createFormData("document", file.name, requestFile)
            val chatIdBody = config.chatId.toRequestBody("text/plain".toMediaType())
            val captionBody = caption?.toRequestBody("text/plain".toMediaType())

            val response = api.sendDocument(
                config.botToken,
                chatIdBody,
                filePart,
                captionBody
            )

            if (response.ok) {
                "File '${file.name}' sent to Telegram."
            } else {
                "Failed to send file to Telegram."
            }
        } catch (e: Exception) {
            "Telegram error: ${e.message}"
        }
    }

    /**
     * Poll for new messages from Telegram.
     * Returns list of new text commands received.
     */
    suspend fun pollUpdates(): List<String> = withContext(Dispatchers.IO) {
        if (!isConnected()) return@withContext emptyList()

        try {
            val response = api.getUpdates(
                config.botToken,
                offset = if (lastUpdateId > 0) lastUpdateId + 1 else null,
                timeout = 5
            )

            if (response.ok && response.result != null) {
                val commands = mutableListOf<String>()

                for (update in response.result) {
                    lastUpdateId = update.updateId

                    val text = update.message?.text
                    val chatId = update.message?.chat?.id?.toString()

                    // Only process messages from the authorized chat
                    if (text != null && chatId == config.chatId) {
                        commands.add(text)
                    }
                }

                // Persist last update ID
                context.dataStore.edit { prefs ->
                    prefs[KEY_LAST_UPDATE_ID] = lastUpdateId
                }

                commands
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
