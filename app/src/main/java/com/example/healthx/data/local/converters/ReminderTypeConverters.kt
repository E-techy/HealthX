package com.example.healthx.data.local.converters

import androidx.room.TypeConverter
import com.example.healthx.data.local.entities.SyncState
import com.example.healthx.data.models.reminders.core.AlarmConfig
import com.example.healthx.data.models.reminders.core.RepeatRule
import com.google.gson.Gson

class ReminderTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromAlarmConfig(config: AlarmConfig): String = gson.toJson(config)
    @TypeConverter
    fun toAlarmConfig(json: String): AlarmConfig = gson.fromJson(json, AlarmConfig::class.java)

    @TypeConverter
    fun fromRepeatRule(rule: RepeatRule): String = gson.toJson(rule)
    @TypeConverter
    fun toRepeatRule(json: String): RepeatRule = gson.fromJson(json, RepeatRule::class.java)

    @TypeConverter
    fun fromSyncState(state: SyncState): String = state.name
    @TypeConverter
    fun toSyncState(name: String): SyncState = SyncState.valueOf(name)
}