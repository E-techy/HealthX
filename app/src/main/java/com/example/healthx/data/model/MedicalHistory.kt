package com.example.healthx.data.model
import java.util.Date

data class MedicalHistory(
    val chronicDiseases: List<DiseaseRecord> = emptyList(),
    val pastSurgeries: List<SurgeryRecord> = emptyList(),
    val clinicalInteractions: List<ClinicalInteraction> = emptyList()
)
data class DiseaseRecord(val diseaseId: String, val name: String, val diagnosedDate: Date, val severity: ConditionSeverity, val isChronic: Boolean = true, val status: DiseaseStatus = DiseaseStatus.ACTIVE)
data class SurgeryRecord(val surgeryName: String, val date: Date, val hospitalName: String, val surgeonName: String?)
data class ClinicalInteraction(val interactionId: String, val doctorName: String, val specialization: String, val hospitalOrClinicName: String, val visitDate: Date, val primarySymptoms: List<String>, val diagnosis: String?, val prescribedMedicationIds: List<String> = emptyList())
enum class ConditionSeverity { MILD, MODERATE, SEVERE }
enum class DiseaseStatus { ACTIVE, IN_REMISSION, RESOLVED }