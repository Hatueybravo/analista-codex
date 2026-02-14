package com.analista.fileorganizer.service.telegram

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface TelegramApi {

    @GET("bot{token}/getUpdates")
    suspend fun getUpdates(
        @Path("token") token: String,
        @Query("offset") offset: Long? = null,
        @Query("timeout") timeout: Int = 10
    ): TelegramResponse<List<Update>>

    @POST("bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path("token") token: String,
        @Body body: SendMessageBody
    ): TelegramResponse<Message>

    @Multipart
    @POST("bot{token}/sendDocument")
    suspend fun sendDocument(
        @Path("token") token: String,
        @Part("chat_id") chatId: RequestBody,
        @Part document: MultipartBody.Part,
        @Part("caption") caption: RequestBody? = null
    ): TelegramResponse<Message>

    @GET("bot{token}/getMe")
    suspend fun getMe(
        @Path("token") token: String
    ): TelegramResponse<User>
}

// --- Data classes ---

data class TelegramResponse<T>(
    val ok: Boolean,
    val result: T?
)

data class Update(
    @SerializedName("update_id") val updateId: Long,
    val message: Message?
)

data class Message(
    @SerializedName("message_id") val messageId: Long,
    val from: User?,
    val chat: Chat?,
    val text: String?,
    val date: Long
)

data class User(
    val id: Long,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String?,
    val username: String?
)

data class Chat(
    val id: Long,
    val type: String,
    val title: String?,
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("last_name") val lastName: String?
)

data class SendMessageBody(
    @SerializedName("chat_id") val chatId: String,
    val text: String,
    @SerializedName("parse_mode") val parseMode: String = "HTML"
)
