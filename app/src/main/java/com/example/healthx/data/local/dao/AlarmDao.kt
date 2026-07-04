package com.example.healthx.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.healthx.data.local.entities.AlarmEntity
import com.example.healthx.data.local.entities.ActiveAlarmLedger
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    // --- ALARM ENTITY CRUD ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarms(alarms: List<AlarmEntity>)

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Query("UPDATE alarms SET status = :newStatus WHERE id = :alarmId")
    suspend fun updateAlarmStatus(alarmId: Int, newStatus: String)

    // FIX: Added missing snooze state updater
    @Query("UPDATE alarms SET isSnoozed = :isSnoozed WHERE id = :alarmId")
    suspend fun updateSnoozeState(alarmId: Int, isSnoozed: Boolean)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :alarmId")
    suspend fun deleteAlarmById(alarmId: Int)

    @Query("DELETE FROM alarms WHERE status = 'CANCELLED'")
    suspend fun cleanupCancelledAlarms()

    // FIX: Added missing fetchers
    @Query("SELECT * FROM alarms WHERE id = :alarmId LIMIT 1")
    suspend fun getAlarmById(alarmId: Int): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getAlarmByRemoteId(remoteId: String): AlarmEntity?

    // --- DYNAMIC ROLLING SCHEDULE QUERY ---
    @Query("""
        SELECT * FROM alarms 
        WHERE status = 'PENDING' 
        AND triggerTimeMillis >= :currentTime 
        AND triggerTimeMillis <= :dynamicEndTimeMillis
        ORDER BY triggerTimeMillis ASC
    """)
    suspend fun getUpcomingRollingAlarms(currentTime: Long, dynamicEndTimeMillis: Long): List<AlarmEntity>


    // --- ACTIVE ALARM LEDGER CRUD ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntoLedger(ledgerEntry: ActiveAlarmLedger)

    @Query("SELECT * FROM active_alarm_ledger")
    suspend fun getAllActiveLedgerEntries(): List<ActiveAlarmLedger>

    @Query("DELETE FROM active_alarm_ledger WHERE alarmId = :alarmId")
    suspend fun removeFromLedger(alarmId: Int)

    @Query("DELETE FROM active_alarm_ledger")
    suspend fun clearEntireLedger()
}