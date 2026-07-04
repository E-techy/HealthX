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

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Query("UPDATE alarms SET status = :newStatus WHERE id = :alarmId")
    suspend fun updateAlarmStatus(alarmId: Int, newStatus: String)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    // --- DYNAMIC ROLLING SCHEDULE QUERY ---
    // UPDATED: Now uses a dynamic end time instead of a hardcoded 24 hours.
    @Query("""
        SELECT * FROM alarms 
        WHERE status = 'PENDING' 
        AND triggerTimeMillis >= :currentTime 
        AND triggerTimeMillis <= :dynamicEndTimeMillis
        ORDER BY triggerTimeMillis ASC
    """)
    suspend fun getUpcomingRollingAlarms(currentTime: Long, dynamicEndTimeMillis: Long): List<AlarmEntity>


    // --- NEW: ACTIVE ALARM LEDGER CRUD ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntoLedger(ledgerEntry: ActiveAlarmLedger)

    @Query("SELECT * FROM active_alarm_ledger")
    suspend fun getAllActiveLedgerEntries(): List<ActiveAlarmLedger>

    @Query("DELETE FROM active_alarm_ledger WHERE alarmId = :alarmId")
    suspend fun removeFromLedger(alarmId: Int)

    @Query("DELETE FROM active_alarm_ledger")
    suspend fun clearEntireLedger()
}