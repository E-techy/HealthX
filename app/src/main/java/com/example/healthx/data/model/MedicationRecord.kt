package com.example.healthx.data.model
import java.util.Date

data class MedicationRecord(
    val medicationId: String,
    val tradeName: String,
    val chemicalComponents: List<String>,
    val manufacturer: String? = null,
    val batchNumber: String? = null,
    val expiryDate: Date? = null,
    val acquisitionType: AcquisitionType,
    val prescribedByDoctorId: String? = null,
    val associatedDiseaseId: String? = null,
    val dosage: DosageInstructions,
    val schedule: MedicationSchedule,
    val startDate: Date,
    val endDate: Date?,
    val isActive: Boolean = true
)
enum class AcquisitionType { PRESCRIBED_CLINICAL, OVER_THE_COUNTER, SUPPLEMENT }
data class DosageInstructions(val amount: Double, val unit: String, val intakeMethod: IntakeMethod, val mealDependency: MealDependency)
enum class IntakeMethod { ORAL, INTRAVENOUS, TOPICAL, INHALATION, INJECTION }
enum class MealDependency { BEFORE_MEAL, WITH_MEAL, AFTER_MEAL, INDEPENDENT }
data class MedicationSchedule(val frequency: FrequencyType, val specificTimesOfDay: List<String>, val daysOfWeek: List<Int> = emptyList())
enum class FrequencyType { DAILY, WEEKLY, AS_NEEDED_PRN, SPECIFIC_DAYS }