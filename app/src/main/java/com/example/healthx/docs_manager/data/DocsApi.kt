package com.example.healthx.docs_manager.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface DocsApi {

    @Multipart
    @POST("api/docs/upload")
    suspend fun uploadDocument(
        @Header("Authorization") token: String,
        @Part documentFile: MultipartBody.Part,
        @Part("documentName") documentName: RequestBody?,
        @Part("documentCategory") documentCategory: RequestBody?
    ): Response<SimpleResponse>

    @GET("api/docs/my-docs")
    suspend fun getMyDocs(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("sort") sort: String,
        @Query("category") category: String?
    ): Response<DocsListResponse>

    @GET("api/docs/shared-with-me")
    suspend fun getSharedWithMe(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("sort") sort: String,
        @Query("category") category: String?
    ): Response<DocsListResponse>

    @GET("api/docs/{documentId}/access-details")
    suspend fun getAccessDetails(
        @Header("Authorization") token: String,
        @Path("documentId") documentId: String
    ): Response<AccessDetailsResponse>

    @POST("api/docs/{documentId}/make-public")
    suspend fun makePublic(
        @Header("Authorization") token: String,
        @Path("documentId") documentId: String
    ): Response<SimpleResponse>

    @POST("api/docs/{documentId}/revoke-public")
    suspend fun revokePublic(
        @Header("Authorization") token: String,
        @Path("documentId") documentId: String
    ): Response<SimpleResponse>

    @POST("api/docs/{documentId}/set-password")
    suspend fun setPassword(
        @Header("Authorization") token: String,
        @Path("documentId") documentId: String,
        @Body request: PasswordRequest
    ): Response<SimpleResponse>

    @POST("api/docs/{documentId}/share")
    suspend fun shareWithUser(
        @Header("Authorization") token: String,
        @Path("documentId") documentId: String,
        @Body request: ShareRequest
    ): Response<SimpleResponse>

    @POST("api/docs/{documentId}/revoke-share")
    suspend fun revokeShare(
        @Header("Authorization") token: String,
        @Path("documentId") documentId: String,
        @Body request: ShareRequest
    ): Response<SimpleResponse>

    @PUT("api/docs/{documentId}")
    suspend fun updateDocument(
        @Header("Authorization") token: String,
        @Path("documentId") documentId: String,
        @Body request: UpdateDocRequest
    ): Response<SimpleResponse>

    @DELETE("api/docs/{documentId}")
    suspend fun deleteDocument(
        @Header("Authorization") token: String,
        @Path("documentId") documentId: String
    ): Response<SimpleResponse>

    // DOWNLOAD ROUTES
    @Streaming
    @GET("api/docs/shared/{documentId}")
    suspend fun downloadSharedDoc(
        @Header("Authorization") token: String,
        @Path("documentId") documentId: String
    ): Response<ResponseBody>

    @Streaming
    @POST("api/docs/secure/{documentId}")
    suspend fun downloadSecureDoc(
        @Path("documentId") documentId: String,
        @Body request: PasswordRequest
    ): Response<ResponseBody>
}