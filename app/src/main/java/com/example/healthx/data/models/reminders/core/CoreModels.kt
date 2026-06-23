package com.example.healthx.data.models.reminders.core

import java.util.UUID

// --- Enums ---
enum class AudioType { DEFAULT, AI_VOICE, USER_CUSTOM }
enum class RepeatType { ONCE, DAILY, WEEKLY, SPECIFIC_DAYS, RANGE_WITH_STEP }
enum class DocumentType { PRESCRIPTION, LAB_REPORT, BILLING, VACCINE_CERT, OTHER }

// --- Reusable Utilities ---

/**
 * Tracks files (PDFs, Images).
 * If [localDeviceUri] is null, the app UI should show a "Download from Cloud" button using [remoteCloudUrl].
 */
data class DocumentAttachment(
    val id: String = UUID.randomUUID().toString(),
    val documentType: DocumentType,
    val fileName: String,
    val remoteCloudUrl: String? = null,    // E.g., hospital website report link
    val localDeviceUri: String? = null,    // E.g., content://... on Android
    val fileSizeKb: Long? = null,
    val downloadedAt: Long? = null
)

/**
 * Stores all contact and location info for a Hospital, Clinic, or Lab.
 */
data class FacilityDetails(
    val facilityId: String? = null,
    val facilityName: String,
    val websiteUrl: String? = null,
    val primaryPhone: String? = null,
    val emergencyPhone: String? = null,
    val physicalAddress: String? = null,
    val googleMapsUrl: String? = null
)

/**
 * Stores all contact info for a Pharmacy or Medical Store.
 */
data class PharmacyDetails(
    val pharmacyId: String? = null,
    val storeName: String,
    val websiteUrl: String? = null,
    val contactPhone: String? = null,
    val physicalAddress: String? = null,
    val is24Hours: Boolean = false
)

data class AlarmConfig(
    val audioType: AudioType = AudioType.DEFAULT,
    val localAudioUri: String? = null,
    val cloudAudioUrl: String? = null,
    val isVibrationEnabled: Boolean = true,
    val volumeLevel: Float = 1.0f
)

data class RepeatRule(
    val repeatType: RepeatType,
    val specificDays: List<Int>? = null,
    val rangeStartDay: Int? = null,
    val rangeEndDay: Int? = null,
    val intervalStep: Int? = null
)

// --- The Master Template ---
interface BaseReminder {
    val id: String
    val userId: String
    val category: String
    val title: String
    val description: String?
    val triggerDateTime: Long
    val alarmConfig: AlarmConfig
    val repeatRule: RepeatRule
    val isActive: Boolean
    val createdAt: Long
    val updatedAt: Long
}