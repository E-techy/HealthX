package com.example.healthx.alarm_manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        // Listen for both standard boot and locked boot (before user enters PIN)
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.LOCKED_BOOT_COMPLETED") {
            Log.d(TAG, "Device rebooted. Initializing HealthX alarm recovery...")

            // goAsync() tells the OS to keep this BroadcastReceiver alive
            // while we do background work (like reading Room DB).
            val pendingResult = goAsync()

            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            scope.launch {
                try {
                    // Call the engine to repopulate the OS AlarmManager
                    val engine = RollingScheduleEngine(context)
                    engine.updateOSAlarms()
                    Log.d(TAG, "✅ Alarm recovery complete. All alarms rescheduled.")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to reschedule alarms on boot: ${e.message}")
                } finally {
                    // Always call finish() so the OS knows it can recycle the receiver
                    pendingResult.finish()
                }
            }
        }
    }
}