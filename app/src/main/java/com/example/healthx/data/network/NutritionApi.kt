package com.example.healthx.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import com.example.healthx.data.model.AnalyzeNutritionResponse
import com.example.healthx.data.model.MealHistoryResponse

interface NutritionApi {

    @Multipart
    @POST("api/nutrition/ai/analyze")
    suspend fun analyzeFoodImages(
        @Header("Authorization") token: String,
        @Part images: List<MultipartBody.Part>,
        @Part("apiKey") apiKey: RequestBody? = null,
        @Part("modelName") modelName: RequestBody? = null,
        @Part("userInputAmount") userInputAmount: RequestBody? = null,
        @Part("userProfile") userProfile: RequestBody? = null
    ): Response<AnalyzeNutritionResponse>

    // NEW: Retrieve Meals History
    @GET("api/nutrition/meals")
    suspend fun getMealsHistory(
        @Header("Authorization") token: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20,
        @Query("date") date: String? = null,
        @Query("mealId") mealId: String? = null,
        @Query("show") show: String? = null
    ): Response<MealHistoryResponse>
}