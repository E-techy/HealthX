package com.example.healthx.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks the exact alarms currently queued in the Android OS AlarmManager.
 * Helps prevent duplicate scheduling, missed alarms during reboots, and ghost alarms.
 */
@Entity(tableName = "active_alarm_ledger")
data class ActiveAlarmLedger(
    @PrimaryKey
    val alarmId: Int, // Foreign key linking back to AlarmEntity.id

    val scheduledTimeMillis: Long, // The exact time given to the OS
    val isSnoozed: Boolean = false // If this specific queue entry is a snooze
)