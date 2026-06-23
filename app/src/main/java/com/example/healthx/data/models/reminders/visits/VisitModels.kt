package com.example.healthx.data.models.reminders.visits

import com.example.healthx.data.models.reminders.core.*
import java.util.UUID

// 4. Doctor Consultation Reminders
data class ConsultationReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "CONSULTATION",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val doctorName: String,
    val specialty: String? = null,
    val doctorRegistrationId: String? = null,

    // Logistics
    val isTelehealth: Boolean,
    val meetingWebUrl: String? = null,
    val meetingPassword: String? = null,
    val facilityDetails: FacilityDetails? = null, // For physical visits

    // Pre/Post Visit
    val symptomsToDiscuss: String? = null,
    val questionsForDoctor: String? = null,

    // Files (e.g., X-Rays to show the doctor, or the prescription received after)
    val attachedDocuments: List<DocumentAttachment> = emptyList()
) : BaseReminder

// 5. Health Checkup Reminders (Annual/Routine)
data class CheckupReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "CHECKUP",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val checkupPackageName: String, // e.g., "Full Body Preventive"
    val facilityDetails: FacilityDetails? = null,

    val fastingRequiredHours: Int? = null,
    val preCheckupInstructions: String? = null,

    val checkupReports: List<DocumentAttachment> = emptyList() // The final results
) : BaseReminder

// 6. Lab Test Reminders
data class LabTestReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "LAB_TEST",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val testName: String, // e.g., "CBC", "Lipid Profile"
    val isHomeCollection: Boolean,
    val homeCollectionAddress: String? = null,
    val phlebotomistName: String? = null, // Person collecting the blood

    val facilityDetails: FacilityDetails? = null, // If going to the lab

    val preparationNotes: String? = null, // e.g., "Drink 2L water prior"
    val expectedReportDate: Long? = null,
    val labReportDocuments: List<DocumentAttachment> = emptyList()
) : BaseReminder

// 7. Therapy Session Reminders
data class TherapyReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "THERAPY",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val therapistName: String,
    val sessionType: String, // e.g., "Physiotherapy", "Psychotherapy"
    val isTelehealth: Boolean,
    val meetingWebUrl: String? = null,
    val facilityDetails: FacilityDetails? = null,

    // Tracking progress
    val preSessionHomeworkNotes: String? = null,
    val postSessionActionItems: String? = null,
    val moodBeforeSession: Int? = null, // 1-10 scale
    val moodAfterSession: Int? = null   // 1-10 scale
) : BaseReminder

// 8. Vaccination / Immunization Reminders
data class VaccinationReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "VACCINATION",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val vaccineName: String,
    val targetDisease: String,
    val manufacturerName: String? = null,
    val batchNumber: String? = null,

    val doseNumber: Int,
    val totalDosesRequired: Int,
    val nextDoseExpectedDate: Long? = null,

    val administeredByDoctorName: String? = null,
    val facilityDetails: FacilityDetails? = null,

    val vaccinationCertificate: DocumentAttachment? = null
) : BaseReminder