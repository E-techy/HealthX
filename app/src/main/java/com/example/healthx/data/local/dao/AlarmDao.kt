package com.example.healthx.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.healthx.data.local.entities.AlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    // --- Create ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarms(alarms: List<AlarmEntity>)

    // --- Update ---
    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Query("UPDATE alarms SET status = :newStatus WHERE id = :alarmId")
    suspend fun updateAlarmStatus(alarmId: Int, newStatus: String)

    @Query("UPDATE alarms SET isSnoozed = :isSnoozed WHERE id = :alarmId")
    suspend fun updateSnoozeState(alarmId: Int, isSnoozed: Boolean)

    // --- Delete ---
    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :alarmId")
    suspend fun deleteAlarmById(alarmId: Int)

    @Query("DELETE FROM alarms WHERE remoteId = :remoteId")
    suspend fun deleteAlarmByRemoteId(remoteId: String)

    @Query("DELETE FROM alarms WHERE status = 'CANCELLED'")
    suspend fun cleanupCancelledAlarms()

    // --- Read ---
    @Query("SELECT * FROM alarms WHERE id = :alarmId LIMIT 1")
    suspend fun getAlarmById(alarmId: Int): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getAlarmByRemoteId(remoteId: String): AlarmEntity?

    // 🌟 ROLLING SCHEDULE ENGINE QUERY 🌟
    // Fetches all pending alarms scheduled within the next 24-hour window.
    @Query("""
        SELECT * FROM alarms 
        WHERE status = 'PENDING' 
        AND triggerTimeMillis >= :currentTime 
        AND triggerTimeMillis <= :twentyFourHoursFromNow
        ORDER BY triggerTimeMillis ASC
    """)
    suspend fun getUpcomingRollingAlarms(currentTime: Long, twentyFourHoursFromNow: Long): List<AlarmEntity>

    // UI Flow: Observe all active alarms for a list screen
    @Query("SELECT * FROM alarms WHERE status = 'PENDING' ORDER BY triggerTimeMillis ASC")
    fun getPendingAlarmsFlow(): Flow<List<AlarmEntity>>
}