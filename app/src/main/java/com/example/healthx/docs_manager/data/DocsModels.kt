package com.example.healthx.docs_manager.data

data class DocumentDto(
    val _id: String,
    val userId: String,
    val documentName: String,
    val documentType: String,
    val documentCategory: String,
    val isPublic: Boolean = false,
    val isPasswordProtected: Boolean = false,
    val sharedCount: Int = 0,
    val createdAt: String
)

data class DocsListResponse(
    val success: Boolean,
    val data: List<DocumentDto>,
    val pagination: PaginationMeta,
    val message: String? = null
)

data class PaginationMeta(
    val totalDocuments: Int,
    val currentPage: Int,
    val totalPages: Int,
    val hasNextPage: Boolean
)

data class AccessDetailsResponse(
    val success: Boolean,
    val data: AccessDetailsData
)

data class AccessDetailsData(
    val isPublic: Boolean,
    val publicUrl: String?,
    val isPasswordProtected: Boolean,
    val sharedUsers: List<SharedUser>
)

data class SharedUser(
    val _id: String,
    val name: String,
    val email: String,
    val profileImageUri: String?
)

data class SimpleResponse(
    val success: Boolean,
    val message: String,
    val publicUrl: String? = null,
    val publicKey: String? = null
)

data class PasswordRequest(val password: String)
data class ShareRequest(val targetUserId: String)
data class UpdateDocRequest(val documentName: String?, val documentCategory: String?)