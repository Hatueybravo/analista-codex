package com.analista.fileorganizer.service.telegram

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.analista.fileorganizer.FileOrganizerApp
import com.analista.fileorganizer.data.model.CommandSource
import com.analista.fileorganizer.service.agent.AgentExecutor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class TelegramPollingService : Service() {

    @Inject
    lateinit var telegramBot: TelegramBot

    @Inject
    lateinit var agentExecutor: AgentExecutor

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isPolling = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        if (!isPolling) {
            isPolling = true
            startPolling()
        }

        return START_STICKY
    }

    private fun startPolling() {
        serviceScope.launch {
            while (isActive && isPolling) {
                try {
                    val commands = telegramBot.pollUpdates()
                    for (commandText in commands) {
                        // Execute the command
                        val result = agentExecutor.execute(commandText, CommandSource.TELEGRAM)
                        // Send the result back via Telegram
                        telegramBot.sendMessage("Result:\n$result")
                    }
                } catch (e: Exception) {
                    // Log and continue polling
                }
                delay(3000) // Poll every 3 seconds
            }
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, FileOrganizerApp.CHANNEL_TELEGRAM)
            .setContentTitle("Telegram Agent")
            .setContentText("Listening for commands via Telegram...")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        isPolling = false
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1002
    }
}
