package com.example.healthx.data.network

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import com.example.healthx.BuildConfig

// Add profilePhotoUrl to the request so users can send it during signup
data class AuthRequest(
    val email: String,
    val password: String? = null,
    val name: String? = null,
    val otp: String? = null,
    val newPassword: String? = null,
    val profilePhotoUrl: String? = null // ADDED THIS
)

data class AuthResponse(val success: Boolean, val message: String, val token: String?, val user: UserDto?)


data class UserDto(
    val accountId: String,
    val email: String,
    val name: String,
    val profilePhotoUrl: String? = null // ADDED THIS
)
interface AuthApi {
    @POST("signup")
    suspend fun signup(@Body req: AuthRequest): Response<AuthResponse>

    @POST("verify-otp")
    suspend fun verifyOtp(@Body req: AuthRequest): Response<AuthResponse>

    @POST("login")
    suspend fun login(@Body req: AuthRequest): Response<AuthResponse>

    @POST("forgot-password")
    suspend fun forgotPassword(@Body req: AuthRequest): Response<AuthResponse>

    @POST("reset-password")
    suspend fun resetPassword(@Body req: AuthRequest): Response<AuthResponse>

    companion object {
        fun create(): AuthApi {
            return Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AuthApi::class.java)
        }
    }
}