package com.example.healthx.data.models.reminders.pharmacy

import com.example.healthx.data.models.reminders.core.*
import java.util.UUID

enum class MealTiming { BEFORE_MEAL, WITH_MEAL, AFTER_MEAL, INDEPENDENT }
enum class MedicineForm { TABLET, CAPSULE, SYRUP, INJECTION, DROPS, INHALER, OINTMENT }

// 1. Medication Reminders
data class MedicationReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "MEDICATION",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    // Medicine specifics
    val medicineName: String,
    val genericName: String? = null,
    val medicineForm: MedicineForm,
    val mealTiming: MealTiming,
    val dosageAmount: Double,
    val dosageUnit: String, // e.g., "mg", "ml"

    // Safety & Instructions
    val specificInstructions: String? = null, // e.g., "Do not take with dairy"
    val sideEffectsToWatch: String? = null,
    val contraindications: String? = null,

    // Origins & Grouping
    val prescriptionId: String? = null,
    val prescribingDoctorName: String? = null,
    val associatedMedicineIds: List<String> = emptyList(), // IDs of meds taken at the exact same time

    // Attached Data
    val pharmacyDetails: PharmacyDetails? = null,
    val prescriptionDocument: DocumentAttachment? = null
) : BaseReminder

// 2. Supplement Reminders
data class SupplementReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "SUPPLEMENT",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val supplementName: String,
    val brandName: String? = null,
    val medicineForm: MedicineForm,
    val purpose: String? = null, // e.g., "Immunity", "Hair Growth"
    val dosageAmount: Double,
    val dosageUnit: String,
    val isWithFood: Boolean,

    // Cycling (e.g., take for 3 months, break for 1 month)
    val cycleDurationDays: Int? = null,
    val breakDurationDays: Int? = null
) : BaseReminder

// 3. Pharmacy Refill Reminders
data class RefillReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "REFILL",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val medicineName: String,
    val rxNumber: String? = null, // Prescription serial number

    // Inventory Tracking
    val totalPillsInBottle: Int,
    val currentPillCount: Int,
    val warningThreshold: Int, // e.g., alert when 5 pills left
    val dailyConsumptionRate: Double, // Used by the app to auto-calculate when it will run out

    // Vendor Data
    val pharmacyDetails: PharmacyDetails? = null,
    val autoReorderWebUrl: String? = null, // Deep link to 1mg, Apollo, etc.
    val lastRefillDate: Long? = null
) : BaseReminder