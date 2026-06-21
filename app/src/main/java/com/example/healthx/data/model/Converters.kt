package com.example.healthx.data.model

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toStringList(list: List<String>): String = gson.toJson(list)

    @TypeConverter
    fun fromContactInfo(value: String): ContactInfo = gson.fromJson(value, ContactInfo::class.java)

    @TypeConverter
    fun toContactInfo(contactInfo: ContactInfo): String = gson.toJson(contactInfo)

    @TypeConverter
    fun fromVitalStats(value: String): VitalStats = gson.fromJson(value, VitalStats::class.java)

    @TypeConverter
    fun toVitalStats(vitalStats: VitalStats): String = gson.toJson(vitalStats)

    @TypeConverter
    fun fromMedicalHistory(value: String): MedicalHistory = gson.fromJson(value, MedicalHistory::class.java)

    @TypeConverter
    fun toMedicalHistory(history: MedicalHistory): String = gson.toJson(history)

    @TypeConverter
    fun fromMedicationList(value: String): List<MedicationRecord> {
        val listType = object : TypeToken<List<MedicationRecord>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toMedicationList(list: List<MedicationRecord>): String = gson.toJson(list)
}