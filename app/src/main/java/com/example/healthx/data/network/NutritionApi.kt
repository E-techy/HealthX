package com.example.healthx.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface NutritionApi {

    @Multipart
    @POST("api/nutrition/ai/analyze")
    suspend fun analyzeFoodImages(
        @Header("Authorization") token: String,
        @Part images: List<MultipartBody.Part>, // Allows sending 1 to 10 images
        @Part("apiKey") apiKey: RequestBody? = null,
        @Part("modelName") modelName: RequestBody? = null,
        @Part("userInputAmount") userInputAmount: RequestBody? = null,
        @Part("userProfile") userProfile: RequestBody? = null // Send as stringified JSON
    ): Response<AnalyzeNutritionResponse>

}