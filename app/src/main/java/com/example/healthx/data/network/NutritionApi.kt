package com.example.healthx.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface NutritionApi {
    @GET("api/nutrition/today")
    suspend fun getTodayDashboard(
        @Header("Authorization") token: String
    ): TodayDashboardResponse

    @Multipart
    @POST("api/nutrition/ai/analyze")
    suspend fun analyzeFoodImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("portionSize") portionSize: RequestBody
    ): AiAnalysisResponse

    @POST("api/nutrition/ai/save")
    suspend fun saveAiMeal(
        @Header("Authorization") token: String,
        @Body request: SaveAiMealRequest
    ): SaveMealResponse
}