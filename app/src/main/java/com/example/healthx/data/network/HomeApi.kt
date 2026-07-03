package com.example.healthx.data.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

data class SubscriptionStatusResponse(
    val success: Boolean,
    val status: String // Expected: "FREE", "PRO", "ULTRA"
)

interface HomeApi {
    @GET("api/subscriptionstatus")
    suspend fun getSubscriptionStatus(@Header("Authorization") token: String): Response<SubscriptionStatusResponse>
}