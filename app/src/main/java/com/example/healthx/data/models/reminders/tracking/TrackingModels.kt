package com.example.healthx.data.models.reminders.tracking

import com.example.healthx.data.models.reminders.core.*
import java.util.UUID

// 15. Vitals & Biometrics Reminders
data class VitalsReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "VITALS",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val vitalType: String, // e.g., "Blood Pressure", "Blood Sugar", "SpO2", "Weight"
    val measurementUnit: String, // e.g., "mmHg", "mg/dL", "kg"
    val requiresEquipment: Boolean = true,

    val preparationInstructions: String? = null, // e.g., "Sit quietly for 5 mins before checking BP"
    val targetNormalRangeMin: Double? = null,
    val targetNormalRangeMax: Double? = null,

    val promptLogToDatabase: Boolean = true // UI will show number inputs directly on the alarm screen
) : BaseReminder

// 16. Symptom Tracking Reminders
data class SymptomReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "SYMPTOM",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val targetSymptom: String, // e.g., "Migraine", "Joint Pain", "Nausea"
    val severityScaleRequired: Boolean = true, // Prompts user to log 1-10 severity

    val promptTriggerLog: Boolean = true, // Prompts "What do you think caused this?"
    val promptReliefActionLog: Boolean = true, // Prompts "Did you take medication/rest?"

    val associatedRemedies: List<String> = emptyList() // E.g., ["Take Ibuprofen", "Dark Room"]
) : BaseReminder

// 17. Cycle & Reproductive Health Reminders
data class CycleReminder(
    override val id: String = UUID.randomUUID().toString(),
    override val userId: String, override val category: String = "CYCLE",
    override val title: String, override val description: String? = null,
    override val triggerDateTime: Long, override val alarmConfig: AlarmConfig,
    override val repeatRule: RepeatRule, override val isActive: Boolean = true,
    override val createdAt: Long = System.currentTimeMillis(), override val updatedAt: Long = System.currentTimeMillis(),

    val phasePrompt: String, // e.g., "Menstrual", "Follicular", "Ovulation", "Luteal"
    val expectedPeriodStartDate: Long? = null,

    val isFertilityWindowWarning: Boolean = false,

    val promptFlowLog: Boolean = true, // "Light", "Medium", "Heavy"
    val promptMoodLog: Boolean = true,
    val promptPhysicalSymptomLog: Boolean = true // e.g., Cramps, Bloating
) : BaseReminder