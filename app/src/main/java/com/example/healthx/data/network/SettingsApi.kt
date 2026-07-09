package com.example.healthx.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT

// Data models reflecting your SETTINGS_DOCS.md structure
data class ApiKeyItem(
    val companyName: String,
    val modelName: String,
    val apiKeyValue: String
)

data class UserSettingsData(
    val _id: String? = null,
    val userId: String? = null,
    val name: String? = null,
    val weight: String? = null,
    val height: String? = null,
    val allergies: List<String> = emptyList(),
    val apiKeys: List<ApiKeyItem> = emptyList(),
    val theme: String = "system",
    val notificationToneUrl: String? = null,
    val profileIcon: String? = null,
    val ethnicity: String? = null,
    val country: String? = null,
    val state: String? = null,
    val preferredLanguage: String? = null
)

data class SettingsResponse(
    val success: Boolean,
    val message: String? = null,
    val data: UserSettingsData
)

interface SettingsApi {
    @GET("api/settings")
    suspend fun getSettings(
        @Header("Authorization") token: String
    ): Response<SettingsResponse>

    @PUT("api/settings")
    suspend fun updateSettings(
        @Header("Authorization") token: String,
        @Body settings: UserSettingsData
    ): Response<SettingsResponse>
}