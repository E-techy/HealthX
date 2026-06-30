package com.example.healthx.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

// --- Models ---

data class SubscriptionPlan(
    val _id: String,
    val planId: String,
    val name: String,
    val shortDescription: String,
    val detailedDescription: String,
    val price: Double,
    val currency: String,
    val billingCycle: String,
    val features: List<String>,
    val isActive: Boolean
)

data class PlanResponse(
    val success: Boolean,
    val count: Int?,
    val data: List<SubscriptionPlan>,
    val message: String? = null
)

data class SinglePlanResponse(
    val success: Boolean,
    val data: SubscriptionPlan?,
    val message: String? = null
)

data class CreateOrderRequest(val subscriptionId: String)

data class OrderData(
    val orderId: String,
    val amount: Int, // In paise
    val currency: String,
    val keyId: String,
    val planName: String,
    val planDescription: String
)

data class CreateOrderResponse(
    val success: Boolean,
    val orderData: OrderData?,
    val message: String? = null
)

data class VerifyPaymentRequest(
    val razorpayOrderId: String,
    val razorpayPaymentId: String,
    val razorpaySignature: String
)

data class BaseResponse(
    val success: Boolean,
    val message: String
)

// --- Retrofit Interface ---

interface SubscriptionApi {
    @GET("api/subscriptions/plans")
    suspend fun getAllPlans(): Response<PlanResponse>

    @GET("api/subscriptions/plans/{id}")
    suspend fun getPlanById(@Path("id") id: String): Response<SinglePlanResponse>

    @POST("api/subscriptions/order/create")
    suspend fun createOrder(
        @Header("Authorization") token: String,
        @Body request: CreateOrderRequest
    ): Response<CreateOrderResponse>

    @POST("api/subscriptions/order/verify")
    suspend fun verifyPayment(
        @Header("Authorization") token: String,
        @Body request: VerifyPaymentRequest
    ): Response<BaseResponse>
}