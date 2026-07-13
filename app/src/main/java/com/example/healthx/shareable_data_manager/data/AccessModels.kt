package com.example.healthx.shareable_data_manager.data

import com.google.gson.annotations.SerializedName

// Standardized API Response Wrapper
data class StandardResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)

// Standard Error Response for 4xx/5xx handling
data class ErrorResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String
)

// Hash Models
data class CreateHashRequest(
    @SerializedName("actions") val actions: List<String>
)

data class UpdateHashStatusRequest(
    @SerializedName("status") val status: String // "ACTIVE" or "UNACTIVE"
)

data class ShareableHash(
    @SerializedName("_id") val id: String,
    @SerializedName("hashId") val hashId: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("actions") val actions: List<String>,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String
)

// Blocklist Model
data class BlocklistedUser(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("profileImageUri") val profileImageUri: String?
)