package com.example.healthx.alarm_manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        Log.d("AlarmReceiver", "Exact Alarm Triggered for ID: $alarmId")

        if (alarmId != -1) {
            val serviceIntent = Intent(context, MediaForegroundService::class.java).apply {
                putExtra("ALARM_ID", alarmId)
            }

            // Must use startForegroundService for Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}