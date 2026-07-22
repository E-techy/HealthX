package com.example.healthx.docs_manager.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthx.data.local.SessionManager
import com.example.healthx.data.network.RetrofitClient
import com.example.healthx.docs_manager.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class DocsViewModel(application: Application) : AndroidViewModel(application) {
    private val api = RetrofitClient.docsApi
    private val sessionManager = SessionManager(application)
    private val context = application.applicationContext

    private val _docsList = MutableStateFlow<List<DocumentDto>>(emptyList())
    val docsList = _docsList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Pagination & Filters
    var currentPage = 1
    var hasNextPage = false
    var currentTab = "MY_DOCS" // or "SHARED"
    var selectedCategory: String? = null
    var searchQuery: String = ""

    init {
        loadDocs(reset = true)
    }

    fun clearError() { _errorMessage.value = null }

    fun loadDocs(reset: Boolean = false) {
        if (reset) {
            currentPage = 1
            _docsList.value = emptyList()
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val account = sessionManager.activeAccountFlow.firstOrNull()
                val token = "Bearer ${account?.token}"

                val response = if (currentTab == "MY_DOCS") {
                    api.getMyDocs(token, currentPage, 20, "desc", selectedCategory)
                } else {
                    api.getSharedWithMe(token, currentPage, 20, "desc", selectedCategory)
                }

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    // Local Search Filtering
                    val filteredData = if (searchQuery.isNotBlank()) {
                        body.data.filter { it.documentName.contains(searchQuery, ignoreCase = true) }
                    } else {
                        body.data
                    }

                    if (reset) _docsList.value = filteredData
                    else _docsList.value = _docsList.value + filteredData

                    hasNextPage = body.pagination.hasNextPage
                    if (hasNextPage) currentPage++
                } else {
                    _errorMessage.value = parseError(response.errorBody()?.string())
                }
            } catch (e: Exception) {
                _errorMessage.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadDocument(uri: Uri, name: String, category: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val account = sessionManager.activeAccountFlow.firstOrNull()
                val token = "Bearer ${account?.token}"

                val file = getFileFromUri(uri)
                if (file == null) {
                    _errorMessage.value = "Failed to process file."
                    _isLoading.value = false
                    return@launch
                }

                val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("documentFile", file.name, requestFile)
                val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val catBody = category.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = api.uploadDocument(token, body, nameBody, catBody)
                if (response.isSuccessful) {
                    loadDocs(reset = true)
                    onSuccess()
                } else {
                    _errorMessage.value = parseError(response.errorBody()?.string())
                }
            } catch (e: Exception) {
                _errorMessage.value = "Upload failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteDocument(docId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = "Bearer ${sessionManager.activeAccountFlow.firstOrNull()?.token}"
                val response = api.deleteDocument(token, docId)
                if (response.isSuccessful) {
                    _docsList.value = _docsList.value.filter { it._id != docId }
                } else {
                    _errorMessage.value = parseError(response.errorBody()?.string())
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    // A helper function to fetch Access Details for the Manager Dialog
    suspend fun getAccessDetails(docId: String): AccessDetailsData? {
        val token = "Bearer ${sessionManager.activeAccountFlow.firstOrNull()?.token}"
        val response = api.getAccessDetails(token, docId)
        return if (response.isSuccessful) response.body()?.data else null
    }

    suspend fun makePublic(docId: String) = api.makePublic("Bearer ${getToken()}", docId)
    suspend fun revokePublic(docId: String) = api.revokePublic("Bearer ${getToken()}", docId)
    suspend fun setPassword(docId: String, pass: String) = api.setPassword("Bearer ${getToken()}", docId, PasswordRequest(pass))
    suspend fun shareUser(docId: String, targetId: String) = api.shareWithUser("Bearer ${getToken()}", docId, ShareRequest(targetId))
    suspend fun revokeShare(docId: String, targetId: String) = api.revokeShare("Bearer ${getToken()}", docId, ShareRequest(targetId))

    private suspend fun getToken() = sessionManager.activeAccountFlow.firstOrNull()?.token

    private fun parseError(errorBody: String?): String {
        return try {
            val json = JSONObject(errorBody ?: "")
            json.getString("message")
        } catch (e: Exception) {
            "An unknown error occurred."
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        // Utility to copy Uri to a temporary file for Retrofit upload
        val contentResolver = context.contentResolver
        val file = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}")
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            null
        }
    }
}