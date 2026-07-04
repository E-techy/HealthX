package com.example.healthx.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // Used as the PendingIntent Request Code

    val remoteId: String?, // The ID from your cloud database (UUID)
    val triggerTimeMillis: Long, // Exact UTC millisecond timestamp

    val audioPlaybackType: String, // Enum: LOCAL_FILE, TTS, SERVER_STREAM
    val localAudioUri: String?, // Path to file if type is LOCAL_FILE
    val ttsContent: String?, // Text to read if type is TTS

    val status: String, // Enum: PENDING, COMPLETED, CANCELLED
    val isSnoozed: Boolean = false, // True if currently in a snooze cycle

    val title: String,
    val description: String,

    // Interval Scheduling
    val isRecurring: Boolean = false,
    val recurrenceType: String?, // DAILY, WEEKLY, MONTHLY
    val recurrenceInterval: Int?, // Every X days/weeks/months
    val recurrenceStartDate: Long?, // UTC start date
    val recurrenceEndDate: Long? // UTC end date
)