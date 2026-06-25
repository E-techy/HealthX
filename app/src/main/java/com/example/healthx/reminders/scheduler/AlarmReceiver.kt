package com.example.healthx.reminders.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Pass all the intent data forward to the Ringing Service
        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            putExtras(intent.extras ?: return)
        }

        // Starts the service that will play music and show the notification
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}