package com.example.healthx.docs_manager.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthx.data.local.SessionManager
import com.example.healthx.data.network.RetrofitClient
import com.example.healthx.docs_manager.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    object Success : DownloadState()
    data class Error(val message: String) : DownloadState()
}

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

    val isPasswordRequired = MutableStateFlow(false)

    var currentPage = 1
    var hasNextPage = false
    var currentTab = "MY_DOCS"
    var selectedCategory: String? = null
    var searchQuery: String = ""

    // --- DOWNLOAD TRACKING ---
    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates = _downloadStates.asStateFlow()
    private val activeDownloadJobs = mutableMapOf<String, Job>()

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "healthx_downloads"

    init {
        createNotificationChannel()
        loadDocs(reset = true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Document Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows progress for downloading documents" }
            notificationManager.createNotificationChannel(channel)
        }
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

                val extension = file.name.substringAfterLast('.', "")
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "application/octet-stream"

                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
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

    // === LIVE DOWNLOAD ENGINE (STORAGE ACCESS FRAMEWORK & NOTIFICATIONS) ===

    fun cancelDownload(docId: String) {
        activeDownloadJobs[docId]?.cancel()
        activeDownloadJobs.remove(docId)
        updateDownloadState(docId, DownloadState.Idle)
        notificationManager.cancel(docId.hashCode())
    }

    private fun updateDownloadState(docId: String, state: DownloadState) {
        _downloadStates.value = _downloadStates.value.toMutableMap().apply { put(docId, state) }
    }

    fun downloadToUri(docId: String, fileName: String, destUri: Uri, context: Context, isShared: Boolean = false) {
        if (activeDownloadJobs.containsKey(docId)) return

        val job = viewModelScope.launch(Dispatchers.IO) {
            updateDownloadState(docId, DownloadState.Downloading(0))
            val notificationId = docId.hashCode()

            // Initialize the persistent notification builder
            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Downloading $fileName")
                .setContentText("0%")
                .setSmallIcon(android.R.drawable.stat_sys_download) // Animated/downloading logo
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, 0, false)

            try {
                val token = "Bearer ${sessionManager.activeAccountFlow.firstOrNull()?.token}"
                val response = api.downloadSharedDoc(token, docId)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val totalBytes = body.contentLength()

                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(8 * 1024)
                            var bytesCopied = 0L
                            var read: Int
                            var lastProgress = 0

                            while (input.read(buffer).also { read = it } >= 0) {
                                yield() // Safely allows cancellation
                                out.write(buffer, 0, read)
                                bytesCopied += read

                                if (totalBytes > 0) {
                                    val progress = ((bytesCopied * 100) / totalBytes).toInt()
                                    if (progress != lastProgress) {
                                        lastProgress = progress
                                        updateDownloadState(docId, DownloadState.Downloading(progress))

                                        // Update the same notification builder
                                        notificationBuilder.setProgress(100, progress, false)
                                            .setContentText("$progress%")
                                        notificationManager.notify(notificationId, notificationBuilder.build())
                                    }
                                }
                            }
                        }
                    }

                    updateDownloadState(docId, DownloadState.Success)

                    // PROPER FIX: Modify the EXACT same builder to strip the progress bar and change the icon
                    notificationBuilder
                        .setContentTitle("Download Complete")
                        .setContentText(fileName)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done) // Static completed logo
                        .setProgress(0, 0, false) // THIS removes the progress bar
                        .setOngoing(false) // Allows the user to swipe it away
                        .setAutoCancel(true)

                    notificationManager.notify(notificationId, notificationBuilder.build())

                    delay(3000)
                    updateDownloadState(docId, DownloadState.Idle)

                } else {
                    updateDownloadState(docId, DownloadState.Error(parseError(response.errorBody()?.string())))
                    notificationManager.cancel(notificationId)
                }
            } catch (e: CancellationException) {
                // Delete the partial file if cancelled mid-download
                context.contentResolver.delete(destUri, null, null)
                updateDownloadState(docId, DownloadState.Idle)
                notificationManager.cancel(notificationId)
            } catch (e: Exception) {
                updateDownloadState(docId, DownloadState.Error(e.localizedMessage ?: "Download failed"))
                notificationManager.cancel(notificationId)
            } finally {
                activeDownloadJobs.remove(docId)
            }
        }
        activeDownloadJobs[docId] = job
    }

    // === CACHED PREVIEW (For the View Button) ===

    fun previewDocument(docId: String, fileName: String, context: Context, isShared: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = "Bearer ${sessionManager.activeAccountFlow.firstOrNull()?.token}"
                val response = api.downloadSharedDoc(token, docId)

                if (response.isSuccessful && response.body() != null) {
                    val safeFileName = fileName.replace(" ", "_")
                    val file = File(context.cacheDir, safeFileName)

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
                        ?: "HealthX_Document_${System.currentTimeMillis()}.pdf"

                    val safeFileName = fileName.replace(" ", "_")
                    val file = File(context.cacheDir, safeFileName)

                    saveToFile(response.body()!!, file)
                    openFileWithIntent(file, response.body()!!.contentType()?.toString(), context)
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

    // INTERNAL UTILS

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

    private fun openFileWithIntent(file: File, serverMimeType: String?, context: Context) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val extension = file.name.substringAfterLast('.', "")
            val computedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: serverMimeType ?: "*/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, computedMimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val resInfoList = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resInfoList.isEmpty()) {
                _errorMessage.value = "No app installed to open this file type."
                return
            }

            for (resolveInfo in resInfoList) {
                context.grantUriPermission(resolveInfo.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Open Document").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(chooser)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to open file: ${e.localizedMessage}"
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        val contentResolver = context.contentResolver
        var originalName = "temp_upload_${System.currentTimeMillis()}"

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    originalName = cursor.getString(index).replace(" ", "_")
                }
            }
        }

        val file = File(context.cacheDir, originalName)
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
}