package com.analista.fileorganizer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.analista.fileorganizer.service.agent.AgentForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, AgentForegroundService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
