package com.example.healthx.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val remoteId: String?,
    val triggerTimeMillis: Long,

    // --- NEW: Categorization & Visuals ---
    val category: String, // e.g., "MEDICATION", "HYDRATION", "APPOINTMENT"
    val logoUrl: String?, // URL for the notification icon

    // --- UPDATED: Audio Logic ---
    // Enum: LOCAL_FILE, TTS, SERVER_STREAM, CLOUD_MEDIA
    val audioPlaybackType: String,
    val localAudioUri: String?,
    val ttsContent: String?,
    val cloudMediaUrl: String?, // NEW: URL to download/stream media

    val status: String,
    val isSnoozed: Boolean = false,

    val title: String,
    val description: String,

    val volumeLevel: Int = 100,

    val isRecurring: Boolean = false,
    val recurrenceType: String?,
    val recurrenceInterval: Int?,
    val recurrenceStartDate: Long?,
    val recurrenceEndDate: Long?
)