package com.example.healthx.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.healthx.data.models.reminders.core.AlarmConfig
import com.example.healthx.data.models.reminders.core.RepeatRule

enum class SyncState { SYNCED, PENDING_UPLOAD, PENDING_DELETE }

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val category: String, // The Discriminator Key
    val title: String,
    val description: String?,
    val triggerDateTime: Long,

    // Stored as JSON strings via TypeConverters
    val alarmConfig: AlarmConfig,
    val repeatRule: RepeatRule,

    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,

    // POLYMORPHIC DATA: Stored as a JSON String (e.g., {"targetVolumeMl": 500, "containerSizeMl": 250})
    val categoryPayload: String,

    // LOCAL ONLY: Tracks offline sync status (Not sent to the backend)
    val syncState: SyncState = SyncState.PENDING_UPLOAD
)