package com.example.healthx.data.models.reminders.care

import com.example.healthx.data.models.reminders.core.*
import java.util.UUID

// 18. Maternity & Pregnancy Reminders
data class MaternityReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "MATERNITY",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val currentTrimester: Int, // 1, 2, or 3
    val pregnancyWeek: Int,
    val taskType: String, // e.g., "Fetal Kick Count", "Kegel Exercises", "Prenatal Vitamin"

    val babyDevelopmentNotes: String? = null, // e.g., "Baby is the size of a lemon"
    val promptContractionTimer: Boolean = false,

    val doctorOrMidwifeDetails: FacilityDetails? = null
) : BaseReminder

// 19. Senior / Elder Care Reminders
data class ElderCareReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "ELDER_CARE",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val elderName: String,
    val careTaskType: String, // e.g., "Mobility Assist", "Hygiene", "Feeding"

    val caregiverInstructions: String? = null, // e.g., "Ensure bed rails are up"
    val emergencyContactPhone: String? = null,

    val promptMoodAndComfortLog: Boolean = true,
    val attachedCarePlanDocument: DocumentAttachment? = null
) : BaseReminder

// 20. Post-Treatment / Recovery Reminders
data class RecoveryReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "RECOVERY",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val conditionOrSurgeryName: String,
    val daysPostOp: Int? = null,
    val taskType: String, // e.g., "Wound Dressing Change", "Ice Pack Application"

    val movementRestrictions: String? = null, // e.g., "Do not lift > 5 lbs"
    val promptPainLevelLog: Boolean = true, // 1-10 pain scale
    val promptWoundPhotoUpload: Boolean = false, // Ask user to take a photo of the healing wound

    val treatingPhysicianDetails: FacilityDetails? = null
) : BaseReminder

// 21. Fully Custom Reminders
data class CustomReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "CUSTOM",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    // UI Customization
    val customIconHexColor: String? = "#3B82F6", // Default Blue
    val customIconName: String? = "ic_custom_star", // References a drawable in Android

    // Actionable link
    val actionWebUrl: String? = null, // Opens a deep link or browser when clicked
    val customTags: List<String> = emptyList(), // User-defined categories

    val attachedDocuments: List<DocumentAttachment> = emptyList()
) : BaseReminder