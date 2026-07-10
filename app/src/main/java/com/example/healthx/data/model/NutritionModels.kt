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
    @SerializedName("mealType") val mealType: String?,
    @SerializedName("foodItems") val foodItems: List<FoodItem>?
)

data class FoodItem(
    @SerializedName("foodName") val foodName: String?,
    @SerializedName("amountTaken") val amountTaken: String?,
    @SerializedName("totalQuantity") val totalQuantity: String?,
    @SerializedName("aiRecommendedQuantity") val aiRecommendedQuantity: String?,
    @SerializedName("mealCategory") val mealCategory: String?, // VEG, NON_VEG, VEGAN, UNKNOWN
    @SerializedName("physicalState") val physicalState: String?,
    @SerializedName("isOrganic") val isOrganic: Boolean?,

    @SerializedName("ingredients") val ingredients: List<String>?,
    @SerializedName("allergens") val allergens: List<String>?,
    @SerializedName("chemicalsOrPreservatives") val chemicalsOrPreservatives: List<String>?,

    // Core Macros
    @SerializedName("totalCalories") val totalCalories: String?,
    @SerializedName("totalProtein") val totalProtein: String?,
    @SerializedName("totalCarbs") val totalCarbs: String?,
    @SerializedName("totalFat") val totalFat: String?,
    @SerializedName("saturatedFat") val saturatedFat: String?,
    @SerializedName("unsaturatedFat") val unsaturatedFat: String?,
    @SerializedName("totalWater") val totalWater: String?,

    @SerializedName("otherNutrients") val otherNutrients: List<Nutrient>?,

    // Extra Data
    @SerializedName("nutritionValuePerUnit") val nutritionValuePerUnit: String?,
    @SerializedName("brandName") val brandName: String?,
    @SerializedName("manufacturerInfo") val manufacturerInfo: String?,
    @SerializedName("manufactureDate") val manufactureDate: String?,
    @SerializedName("expiryDate") val expiryDate: String?,
    @SerializedName("countryOfOrigin") val countryOfOrigin: String?,

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

data class NutritionErrorResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("requiresPro") val requiresPro: Boolean?,
    @SerializedName("error") val error: String?
)