package com.example.healthx.data.local.dao

import androidx.room.*
import com.example.healthx.data.local.entities.ReminderEntity
import com.example.healthx.data.local.entities.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    // Upsert replaces the row if the ID already exists
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminders(reminders: List<ReminderEntity>)

    // For the UI to display active reminders
    @Query("SELECT * FROM reminders WHERE isActive = 1 AND syncState != 'PENDING_DELETE' ORDER BY triggerDateTime ASC")
    fun getActiveReminders(): Flow<List<ReminderEntity>>

    // For the Sync Engine to find what needs uploading
    @Query("SELECT * FROM reminders WHERE syncState = 'PENDING_UPLOAD'")
    suspend fun getPendingUploads(): List<ReminderEntity>

    // Mark as synced after successful upload
    @Query("UPDATE reminders SET syncState = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    // Hard delete after cloud confirms deletion
    @Query("DELETE FROM reminders WHERE id IN (:ids)")
    suspend fun hardDeleteReminders(ids: List<String>)
}