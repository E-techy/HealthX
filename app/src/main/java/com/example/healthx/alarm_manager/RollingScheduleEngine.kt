package com.example.healthx.alarm_manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.healthx.data.local.AppDatabase
import com.example.healthx.data.local.SessionManager
import com.example.healthx.data.local.entities.ActiveAlarmLedger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

class RollingScheduleEngine(private val context: Context) {
    private val TAG = "RollingScheduleEngine"
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val db = AppDatabase.getDatabase(context)
    private val sessionManager = SessionManager(context)

    suspend fun updateOSAlarms() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Running Rolling Schedule Engine...")

        // 1. Get the dynamic cycle window (Default 1 hour if not set)
        // You would typically read this from DataStore/SharedPreferences
        val cycleHours = 1 // Replace with actual fetch: sessionManager.alarmCycleHours.first()
        val cycleMillis = cycleHours * 60 * 60 * 1000L

        val currentTime = System.currentTimeMillis()
        val windowEndTime = currentTime + cycleMillis

        val alarmDao = db.alarmDao()

        // 2. Fetch all one-time alarms falling in this window
        val upcomingAlarms = alarmDao.getUpcomingRollingAlarms(currentTime, windowEndTime).toMutableList()

        // 3. Fetch recurring alarms and calculate their next instance
        // (Simplified for example: In production, you write a helper to calculate exact next dates based on daily/weekly rules)
        // val recurringAlarms = alarmDao.getActiveRecurringAlarms(currentTime)
        // for (alarm in recurringAlarms) {
        //     val nextTrigger = calculateNextRecurrence(alarm, currentTime)
        //     if (nextTrigger in currentTime..windowEndTime) {
        //         upcomingAlarms.add(alarm.copy(triggerTimeMillis = nextTrigger))
        //     }
        // }

        // 4. Clear old hardware alarms from the Ledger
        val currentLedger = alarmDao.getAllActiveLedgerEntries()
        currentLedger.forEach { cancelHardwareAlarm(it.alarmId) }
        alarmDao.clearEntireLedger()

        // 5. Schedule the new batch
        for (alarm in upcomingAlarms) {
            scheduleHardwareAlarm(alarm.id, alarm.triggerTimeMillis)

            // Save to Ledger so we know exactly what is running
            alarmDao.insertIntoLedger(
                ActiveAlarmLedger(
                    alarmId = alarm.id,
                    scheduledTimeMillis = alarm.triggerTimeMillis,
                    isSnoozed = alarm.isSnoozed
                )
            )
        }

        Log.d(TAG, "Scheduled ${upcomingAlarms.size} alarms for the next $cycleHours hours.")
    }

    private fun scheduleHardwareAlarm(alarmId: Int, triggerAtMillis: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Exact scheduling bypassing Doze mode
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing SCHEDULE_EXACT_ALARM permission.")
        }
    }

    fun cancelHardwareAlarm(alarmId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}