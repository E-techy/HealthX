package com.example.healthx.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import com.example.healthx.BuildConfig

data class AuthRequest(val email: String, val password: String? = null, val otp: String? = null, val newPassword: String? = null)
data class AuthResponse(val success: Boolean, val message: String, val token: String?, val user: UserDto?)
data class UserDto(val accountId: String, val email: String, val name: String, val profilePhotoUrl: String? = null)

interface AuthApi {

    // UPDATED: Now uses Multipart for file uploads
    @Multipart
    @POST("signup")
    suspend fun signup(
        @Part("name") name: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part profileImage: MultipartBody.Part? // Optional image file
    ): Response<AuthResponse>

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