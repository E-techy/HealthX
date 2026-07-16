package com.example.healthx.shareable_data_manager.data

import retrofit2.Response
import retrofit2.http.*

interface DelegatedAccessApi {

    // --- Phase 1: Hash Management ---
    @POST("api/access/hash")
    suspend fun createHash(@Header("Authorization") token: String, @Body request: CreateHashRequest): Response<StandardResponse<ShareableHash>>

    @GET("api/access/hash")
    suspend fun getMyHashes(@Header("Authorization") token: String): Response<StandardResponse<List<ShareableHash>>>

    @PATCH("api/access/hash/{hashId}/status")
    suspend fun updateHashStatus(@Header("Authorization") token: String, @Path("hashId") hashId: String, @Body request: UpdateHashStatusRequest): Response<StandardResponse<Unit>>

    @DELETE("api/access/hash/{hashId}")
    suspend fun deleteHash(@Header("Authorization") token: String, @Path("hashId") hashId: String): Response<StandardResponse<Unit>>

    // --- Phase 2: Connect ---
    @POST("api/access/connect/{hashId}")
    suspend fun connectWithHash(@Header("Authorization") token: String, @Path("hashId") hashId: String): Response<StandardResponse<Unit>>

    // --- Phase 3: Dashboard & Management ---
    @GET("api/access/friends/received")
    suspend fun getReceivedAccess(@Header("Authorization") token: String): Response<StandardResponse<List<ReceivedAccessItem>>>

    @GET("api/access/friends/granted")
    suspend fun getGrantedAccess(@Header("Authorization") token: String): Response<StandardResponse<List<GrantedAccessItem>>>

    @PATCH("api/access/friends/{targetUserId}/permissions")
    suspend fun updateFriendPermissions(@Header("Authorization") token: String, @Path("targetUserId") targetUserId: String, @Body request: UpdatePermissionsRequest): Response<StandardResponse<List<ActivePermission>>>

    @POST("api/access/friends/{targetUserId}/block")
    suspend fun blockUser(@Header("Authorization") token: String, @Path("targetUserId") targetUserId: String, @Body request: BlockUserRequest): Response<StandardResponse<Unit>>

    // --- Phase 4: Blocklist Management ---
    @GET("api/access/blocklist")
    suspend fun getBlocklist(@Header("Authorization") token: String): Response<StandardResponse<List<BlocklistedUser>>>

    @DELETE("api/access/blocklist/{blockedUserId}")
    suspend fun unblockUser(@Header("Authorization") token: String, @Path("blockedUserId") blockedUserId: String): Response<StandardResponse<Unit>>
}