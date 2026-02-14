package com.analista.fileorganizer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FileOrganizerApp : Application() {

    companion object {
        const val CHANNEL_AGENT = "agent_service"
        const val CHANNEL_TELEGRAM = "telegram_service"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val agentChannel = NotificationChannel(
                CHANNEL_AGENT,
                "Agent Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for the AI agent background service"
            }

            val telegramChannel = NotificationChannel(
                CHANNEL_TELEGRAM,
                "Telegram Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for Telegram bot messages"
            }

            manager.createNotificationChannel(agentChannel)
            manager.createNotificationChannel(telegramChannel)
        }
    }
}
