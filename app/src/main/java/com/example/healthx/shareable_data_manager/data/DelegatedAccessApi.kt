package com.example.healthx.shareable_data_manager.data

import retrofit2.Response
import retrofit2.http.*

interface DelegatedAccessApi {

    // --- Hash Management ---
    @POST("api/access/hash")
    suspend fun createHash(
        @Header("Authorization") token: String,
        @Body request: CreateHashRequest
    ): Response<StandardResponse<ShareableHash>>

    @GET("api/access/hash")
    suspend fun getMyHashes(
        @Header("Authorization") token: String
    ): Response<StandardResponse<List<ShareableHash>>>

    @PATCH("api/access/hash/{hashId}/status")
    suspend fun updateHashStatus(
        @Header("Authorization") token: String,
        @Path("hashId") hashId: String,
        @Body request: UpdateHashStatusRequest
    ): Response<StandardResponse<Unit>>

    @DELETE("api/access/hash/{hashId}")
    suspend fun deleteHash(
        @Header("Authorization") token: String,
        @Path("hashId") hashId: String
    ): Response<StandardResponse<Unit>>

    // --- Blocklist Management ---
    @GET("api/access/blocklist")
    suspend fun getBlocklist(
        @Header("Authorization") token: String
    ): Response<StandardResponse<List<BlocklistedUser>>>

    @DELETE("api/access/blocklist/{blockedUserId}")
    suspend fun unblockUser(
        @Header("Authorization") token: String,
        @Path("blockedUserId") blockedUserId: String
    ): Response<StandardResponse<Unit>>
}