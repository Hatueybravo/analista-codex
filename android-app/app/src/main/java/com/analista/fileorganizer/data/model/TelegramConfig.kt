package com.analista.fileorganizer.data.model

data class TelegramConfig(
    val botToken: String = "",
    val chatId: String = "",
    val isEnabled: Boolean = false,
    val pollingIntervalMs: Long = 3000
)
