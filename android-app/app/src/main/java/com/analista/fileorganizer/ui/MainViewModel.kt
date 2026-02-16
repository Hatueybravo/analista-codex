package com.analista.fileorganizer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.analista.fileorganizer.data.local.CommandDao
import com.analista.fileorganizer.data.local.FileDao
import com.analista.fileorganizer.data.model.*
import com.analista.fileorganizer.service.agent.AgentExecutor
import com.analista.fileorganizer.service.organizer.FileOrganizer
import com.analista.fileorganizer.service.telegram.TelegramBot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val agentExecutor: AgentExecutor,
    private val fileOrganizer: FileOrganizer,
    private val telegramBot: TelegramBot,
    private val fileDao: FileDao,
    private val commandDao: CommandDao
) : ViewModel() {

    // Files state
    val files: StateFlow<List<FileItem>> = fileDao.getAllFiles()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Commands history
    val commandHistory: StateFlow<List<AgentCommand>> = commandDao.getRecentCommands(50)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Telegram config
    val telegramConfig: StateFlow<TelegramConfig> = telegramBot.configFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, TelegramConfig())

    // UI state
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Agent conversation messages
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    /**
     * Execute an agent command from the chat input.
     */
    fun executeCommand(input: String) {
        if (input.isBlank()) return

        // Add user message
        addChatMessage(ChatMessage(text = input, isUser = true))

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            val result = agentExecutor.execute(input)

            // Add agent response
            addChatMessage(ChatMessage(text = result, isUser = false))

            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    /**
     * Scan downloads folder.
     */
    fun scanDownloads() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = "Scanning downloads...") }
            val files = fileOrganizer.scanDownloads()
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    statusMessage = "Found ${files.size} files"
                )
            }
        }
    }

    /**
     * Organize files by type and date.
     */
    fun organizeFiles(mode: OrganizeMode = OrganizeMode.TYPE_AND_DATE) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = "Organizing files...") }

            val result = when (mode) {
                OrganizeMode.BY_TYPE -> fileOrganizer.organizeByType()
                OrganizeMode.BY_DATE -> fileOrganizer.organizeByDate()
                OrganizeMode.TYPE_AND_DATE -> fileOrganizer.organizeByTypeAndDate()
            }

            _uiState.update {
                it.copy(
                    isProcessing = false,
                    statusMessage = result.summary()
                )
            }
        }
    }

    /**
     * Configure Telegram bot.
     */
    fun configureTelegram(botToken: String, chatId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            telegramBot.saveConfig(botToken, chatId)

            val verifyResult = telegramBot.verifyToken()
            val message = verifyResult.fold(
                onSuccess = { "Telegram connected: $it" },
                onFailure = { "Telegram error: ${it.message}" }
            )

            _uiState.update { it.copy(isProcessing = false, statusMessage = message) }
        }
    }

    fun disconnectTelegram() {
        viewModelScope.launch {
            telegramBot.disconnect()
            _uiState.update { it.copy(statusMessage = "Telegram disconnected") }
        }
    }

    private fun addChatMessage(message: ChatMessage) {
        _chatMessages.update { it + message }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}

data class UiState(
    val isProcessing: Boolean = false,
    val statusMessage: String? = null
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

enum class OrganizeMode {
    BY_TYPE,
    BY_DATE,
    TYPE_AND_DATE
}
