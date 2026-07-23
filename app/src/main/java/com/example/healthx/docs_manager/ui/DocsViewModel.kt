package com.example.healthx.docs_manager.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
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
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.content.pm.PackageManager
import android.os.Environment
import android.widget.Toast

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

    // State for the Public Link Fetcher
    val isPasswordRequired = MutableStateFlow(false)

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

    // === NEW: DOWNLOAD AND PREVIEW ENGINE ===

    fun downloadAndPreviewDocument(docId: String, fileName: String, context: Context, isShared: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = "Bearer ${sessionManager.activeAccountFlow.firstOrNull()?.token}"

                val response = api.downloadSharedDoc(token, docId) // Shared route works for owners too

                if (response.isSuccessful && response.body() != null) {
                    saveAndOpenFile(response.body()!!, fileName, context)
                } else {
                    _errorMessage.value = parseError(response.errorBody()?.string())
                }
            } catch (e: Exception) {
                _errorMessage.value = "Download failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchPublicDocument(publicKey: String, password: String?, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            isPasswordRequired.value = false
            try {
                val response = if (password.isNullOrBlank()) {
                    api.downloadPublicDoc(publicKey)
                } else {
                    api.downloadPublicSecureDoc(publicKey, PasswordRequest(password))
                }

                if (response.isSuccessful && response.body() != null) {
                    val disposition = response.headers()["Content-Disposition"]
                    val fileName = disposition?.substringAfter("filename=\"")?.substringBefore("\"")
                        ?: "HealthX_Document_${System.currentTimeMillis()}"

                    saveAndOpenFile(response.body()!!, fileName, context)
                } else if (response.code() == 401) {
                    isPasswordRequired.value = true
                    _errorMessage.value = "This document is password protected."
                } else {
                    _errorMessage.value = parseError(response.errorBody()?.string())
                }
            } catch (e: Exception) {
                _errorMessage.value = "Fetch failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun saveAndOpenFile(body: ResponseBody, fileName: String, context: Context) {
        try {
            val file = File(context.cacheDir, fileName)
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                inputStream = body.byteStream()
                outputStream = FileOutputStream(file)
                val buffer = ByteArray(4096)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            } finally {
                inputStream?.close()
                outputStream?.close()
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, body.contentType()?.toString() ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

        } catch (e: Exception) {
            _errorMessage.value = "Failed to open file: ${e.localizedMessage}"
        }
    }

    // === END DOWNLOAD ENGINE ===

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
    // ... Inside DocsViewModel.kt


    // === NEW: ROBUST PREVIEW AND DOWNLOAD ENGINE ===

    fun previewDocument(docId: String, fileName: String, context: Context, isShared: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = "Bearer ${sessionManager.activeAccountFlow.firstOrNull()?.token}"
                val response = api.downloadSharedDoc(token, docId)

                if (response.isSuccessful && response.body() != null) {
                    // Preview saves to CacheDir (Cleared when app needs memory)
                    val file = File(context.cacheDir, fileName.replace(" ", "_"))
                    saveToFile(response.body()!!, file)
                    openFileWithIntent(file, response.body()!!.contentType()?.toString(), context)
                } else {
                    _errorMessage.value = parseError(response.errorBody()?.string())
                }
            } catch (e: Exception) {
                _errorMessage.value = "Preview failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun downloadToDevice(docId: String, fileName: String, context: Context, isShared: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = "Bearer ${sessionManager.activeAccountFlow.firstOrNull()?.token}"
                val response = api.downloadSharedDoc(token, docId)

                if (response.isSuccessful && response.body() != null) {
                    // Download saves permanently to the OS Downloads folder
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, fileName.replace(" ", "_"))
                    saveToFile(response.body()!!, file)

                    Toast.makeText(context, "Saved to Downloads: ${file.name}", Toast.LENGTH_LONG).show()
                } else {
                    _errorMessage.value = parseError(response.errorBody()?.string())
                }
            } catch (e: Exception) {
                _errorMessage.value = "Download failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Extracted file writing logic
    private fun saveToFile(body: ResponseBody, file: File) {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        try {
            inputStream = body.byteStream()
            outputStream = FileOutputStream(file)
            val buffer = ByteArray(4096)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            outputStream.flush()
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    // FIX FOR THE HANGING VIEWER
    private fun openFileWithIntent(file: File, mimeType: String?, context: Context) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // EXPLICITLY GRANT PERMISSION TO ALL APPS THAT CAN HANDLE THIS INTENT (Fixes the blank/hanging screen)
            val resInfoList = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            _errorMessage.value = "No app found to open this file type."
        }
    }
}