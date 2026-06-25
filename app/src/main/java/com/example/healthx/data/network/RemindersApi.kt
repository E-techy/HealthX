package com.example.healthx.data.network

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.*

// Request Models
data class SyncRequest(val lastClientSyncTime: Long, val clientPendingUploads: List<JsonObject>)
data class BatchCreateRequest(val reminders: List<JsonObject>)
data class VoiceGenerateRequest(val reminderText: String, val reminderTime: Long, val tone: String, val prompt: String)

// Response Models
data class SyncResponse(val success: Boolean, val serverCurrentTime: Long, val updatedReminders: List<JsonObject>?)
data class StandardResponse(val success: Boolean, val message: String, val insertedIds: List<String>? = null)
data class VoiceResponse(val success: Boolean, val message: String, val audioUrl: String?, val durationSeconds: Double?)
data class GetRemindersResponse(val success: Boolean, val data: List<JsonObject>?)

interface RemindersApi {
    @POST("reminders/sync")
    suspend fun syncReminders(@Body request: SyncRequest): Response<SyncResponse>

    @POST("reminders")
    suspend fun createReminders(@Body request: BatchCreateRequest): Response<StandardResponse>

    @PUT("reminders/{id}")
    suspend fun updateReminder(@Path("id") id: String, @Body request: JsonObject): Response<StandardResponse>

    @GET("reminders")
    suspend fun getAllReminders(): Response<GetRemindersResponse>

    @POST("voice/generate")
    suspend fun generateVoice(@Body request: VoiceGenerateRequest): Response<VoiceResponse>
}