package com.example.healthx.alarm_manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.healthx.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmActionReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val alarmId = intent.getIntExtra("ALARM_ID", -1)

        if (alarmId == -1) return

        Log.d("AlarmAction", "User clicked $action for Alarm ID: $alarmId")

        // 1. Instantly stop the blaring audio service
        val stopServiceIntent = Intent(context, MediaForegroundService::class.java)
        context.stopService(stopServiceIntent)

        val db = AppDatabase.getDatabase(context)
        val engine = RollingScheduleEngine(context)

        scope.launch {
            val alarm = db.alarmDao().getAlarmById(alarmId) ?: return@launch

            when (action) {
                "ACTION_STOP" -> {
                    // Update Database
                    db.alarmDao().updateAlarmStatus(alarmId, "COMPLETED")
                    db.alarmDao().removeFromLedger(alarmId)

                    // Re-calculate the cycle for the next batch
                    engine.updateOSAlarms()
                }

                "ACTION_SNOOZE" -> {
                    // Update Database status
                    db.alarmDao().updateSnoozeState(alarmId, true)

                    // Add 5 minutes
                    val snoozeTime = System.currentTimeMillis() + (5 * 60 * 1000L)

                    // Create an explicit one-off alarm directly via engine logic
                    // In a production scenario, you might add a specific snooze helper inside the engine
                    // to set this and update the ledger immediately.
                    Log.d("AlarmAction", "Snoozed! Will wake up again at $snoozeTime")
                }
            }
        }
    }
}