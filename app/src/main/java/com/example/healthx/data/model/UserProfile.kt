package com.example.healthx.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val userId: String,
    val name: String,
    val profileImageUri: String?,
    val gender: String,
    val dateOfBirth: Date,

    // Complex objects are stored as JSON strings in the local database via TypeConverters
    val contactInfo: ContactInfo,
    val vitalStats: VitalStats,
    val medicalHistory: MedicalHistory,

    val activeMedications: List<MedicationRecord> = emptyList(),
    val historicalMedications: List<MedicationRecord> = emptyList(),

    val encryptedApiKey: String?,
    val ownedChatIds: List<String> = emptyList(),

    val createdAt: Date = Date(),
    val lastSyncTime: Date = Date()
)