package com.example.healthx.data.model

import com.google.gson.annotations.SerializedName

// Success Response Models
data class AnalyzeNutritionResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("mealId") val mealId: String?,
    @SerializedName("imageUrls") val imageUrls: List<String>?,
    @SerializedName("data") val data: NutritionData?
)

data class NutritionData(
    @SerializedName("foodItems") val foodItems: List<FoodItem>?
)

data class FoodItem(
    @SerializedName("foodName") val foodName: String?,
    @SerializedName("amountTaken") val amountTaken: String?,
    @SerializedName("mealCategory") val mealCategory: String?,
    @SerializedName("physicalState") val physicalState: String?,
    @SerializedName("isOrganic") val isOrganic: Boolean?,
    @SerializedName("ingredients") val ingredients: List<String>?,
    @SerializedName("allergens") val allergens: List<String>?,
    @SerializedName("chemicalsOrPreservatives") val chemicalsOrPreservatives: List<String>?,

    // Core macros stored as Strings per your DB schema
    @SerializedName("totalCalories") val totalCalories: String?,
    @SerializedName("totalProtein") val totalProtein: String?,
    @SerializedName("totalCarbs") val totalCarbs: String?,
    @SerializedName("totalFat") val totalFat: String?,

    @SerializedName("otherNutrients") val otherNutrients: List<Nutrient>?,
    @SerializedName("foodScore") val foodScore: Double?,
    @SerializedName("foodScoreReason") val foodScoreReason: String?,
    @SerializedName("aiInsights") val aiInsights: AiInsights?
)

data class Nutrient(
    @SerializedName("name") val name: String?,
    @SerializedName("amount") val amount: String?
)

data class AiInsights(
    @SerializedName("whyGood") val whyGood: List<String>?,
    @SerializedName("whyNot") val whyNot: List<String>?
)

// Error Response Model (for parsing 400, 403, and 500 errors)
data class NutritionErrorResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("requiresPro") val requiresPro: Boolean?,
    @SerializedName("error") val error: String?
)