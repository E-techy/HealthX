package com.example.healthx.ui.screens.nutrition

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Nutrient(
    val name: String,
    val current: Float,
    val target: Float,
    val unit: String,
    val icon: ImageVector,
    val color: Color
)

data class DailyHealthRecord(
    val dayLabel: String,
    val score: Int
)

class NutritionViewModel(application: Application) : AndroidViewModel(application) {

    // Mock Historical Data for the Graph
    private val _weeklyScores = MutableStateFlow(
        listOf(
            DailyHealthRecord("Mon", 78),
            DailyHealthRecord("Tue", 85),
            DailyHealthRecord("Wed", 62),
            DailyHealthRecord("Thu", 90),
            DailyHealthRecord("Fri", 95),
            DailyHealthRecord("Sat", 88),
            DailyHealthRecord("Sun", 91) // Today's Score
        )
    )
    val weeklyScores = _weeklyScores.asStateFlow()

    // Current Day Score
    val todaysScore = _weeklyScores.value.last().score

    fun getScoreEvaluation(score: Int): String = when {
        score >= 90 -> "Excellent"
        score >= 75 -> "Good"
        score >= 60 -> "Fair"
        else -> "Needs Attention"
    }

    fun getScoreColor(score: Int): Color = when {
        score >= 90 -> Color(0xFF4CAF50) // Green
        score >= 75 -> Color(0xFF2196F3) // Blue
        score >= 60 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFE53935)        // Red
    }
}