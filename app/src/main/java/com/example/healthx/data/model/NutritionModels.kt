package com.example.healthx.data.models

data class TodayDashboardResponse(
    val success: Boolean,
    val summary: DailySummary,
    val meals: List<MealEntry>
)

data class DailySummary(
    val totalCalories: Int,
    val totalProtein: Int,
    val totalCarbs: Int,
    val totalFat: Int,
    val dailyHealthScore: Int
)

data class MealEntry(
    val foodName: String,
    val timestamp: Long
)

data class AiAnalysisResponse(
    val success: Boolean,
    val data: AiFoodData
)

data class AiFoodData(
    val foodDetected: String,
    val foodCategory: String,
    val portionAnalyzed: Int,
    val rawNutrientsExtracted: RawNutrients,
    val scores: AiScores,
    val aiInsights: String,
    val allergens: List<String>?,
    val imageUrl: String?
)

data class RawNutrients(
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val sugar: Float,
    val sodium: Float
)

data class AiScores(
    val foodQualityScore: Int,
    val eatRecommendationScore: Int
)

data class SaveAiMealRequest(
    val mealCategory: String = "SNACK",
    val foodName: String,
    val imageUrl: String?,
    val foodQualityScore: Int,
    val aiInsights: String,
    val portionAnalyzed: Int,
    val rawNutrients: RawNutrients,
    val allergens: List<String>?
)

data class SaveMealResponse(
    val success: Boolean
)