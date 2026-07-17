package com.example.healthx.shareable_data_manager.data

import com.google.gson.annotations.SerializedName

// Standardized API Response Wrapper
data class StandardResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)

// Standard Error Response
data class ErrorResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String
)

// Phase 1: Hash Models
data class CreateHashRequest(@SerializedName("actions") val actions: List<String>)
data class UpdateHashStatusRequest(@SerializedName("status") val status: String)

data class ShareableHash(
    @SerializedName("_id") val id: String,
    @SerializedName("hashId") val hashId: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("actions") val actions: List<String>,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String
)

// Phase 2 & 3: Connection & Access Models
data class ActivePermission(
    @SerializedName("action") var action: String,
    @SerializedName("isActive") var isActive: Boolean
)

data class ReceivedAccessProfile(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("profileImageUri") val profileImageUri: String?
)

// People whose data I can see (User B)
data class ReceivedAccessItem(
    @SerializedName("user") val user: ReceivedAccessProfile,
    @SerializedName("activePermissions") val activePermissions: List<ActivePermission>,
    @SerializedName("connectedAt") val connectedAt: String
)

// People who can see MY data (User A)
data class GrantedAccessItem(
    @SerializedName("friendshipId") val friendshipId: String,
    @SerializedName("user") val user: ReceivedAccessProfile,
    @SerializedName("permissions") val permissions: List<ActivePermission>,
    @SerializedName("connectedAt") val connectedAt: String
)

data class UpdatePermissionsRequest(
    @SerializedName("permissions") val permissions: List<ActivePermission>
)

data class BlockUserRequest(
    @SerializedName("reason") val reason: String,
    @SerializedName("notes") val notes: String = ""
)

// Blocklist Model
data class BlocklistedUser(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("profileImageUri") val profileImageUri: String?
)

data class UpdateHashActionsRequest(
    @SerializedName("actions") val actions: List<String>
)