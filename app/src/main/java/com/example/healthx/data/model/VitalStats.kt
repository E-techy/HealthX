package com.example.healthx.data.model

data class VitalStats(
    val bloodGroup: BloodGroup,
    val heightCm: Double,
    val weightKg: Double,
    val bmi: Double = weightKg / ((heightCm / 100.0) * (heightCm / 100.0)),
    val allergies: List<String> = emptyList(),
    val bloodPressureBaseline: String? = null,
    val stepsHistorySummaryId: String? = null,
    val sleepHistorySummaryId: String? = null,
    val calorieHistorySummaryId: String? = null
)
enum class BloodGroup { A_POS, A_NEG, B_POS, B_NEG, AB_POS, AB_NEG, O_POS, O_NEG }