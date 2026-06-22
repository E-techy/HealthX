package com.example.healthx.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthx.data.local.SavedAccount
import com.example.healthx.data.local.SessionManager
import com.example.healthx.data.network.AuthApi
import com.example.healthx.data.network.AuthRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Response
import android.net.Uri
import com.example.healthx.utils.FileHelper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val api = AuthApi.create()
    val sessionManager = SessionManager(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    var pendingVerificationEmail: String = ""
    var resetEmail: String = ""

    // Helper to safely parse 400/401/409 error bodies from the backend
    private fun parseError(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (errorBody != null) {
                JSONObject(errorBody).getString("message")
            } else {
                "An unexpected error occurred."
            }
        } catch (e: Exception) {
            "Server error (HTTP ${response.code()})"
        }
    }

    fun clearError() {
        _authError.value = null
    }

    fun login(email: String, pass: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.login(AuthRequest(email = email, password = pass))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    val account = SavedAccount(
                        accountId = body.user!!.accountId,
                        email = body.user.email,
                        name = body.user.name,
                        token = body.token!!,
                        profilePhotoUrl = formatImageUrl(body.user.profilePhotoUrl)                    )
                    sessionManager.saveAccountAndSetActive(account)
                    onSuccess("Login Successful: Welcome back, ${body.user.name}")
                } else {
                    _authError.value = parseError(response)
                }
            } catch (e: Exception) {
                _authError.value = "Network error: Make sure backend is running."
            } finally { _isLoading.value = false }
        }
    }

    fun loginAsGuest(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val guestAccount = SavedAccount(
                accountId = "guest_${System.currentTimeMillis()}",
                email = "Guest User",
                name = "Guest",
                token = "local_only_mode",
                isGuest = true
            )
            sessionManager.saveAccountAndSetActive(guestAccount)
            onSuccess("Logged in securely as Guest (Local Mode)")
        }
    }

    // Added profilePhotoUrl as an optional parameter for when you implement the image picker
    fun signup(name: String, email: String, pass: String, profilePhotoUri: Uri? = null, onOtpSent: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Convert text fields to RequestBody
                val namePart = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val emailPart = email.toRequestBody("text/plain".toMediaTypeOrNull())
                val passPart = pass.toRequestBody("text/plain".toMediaTypeOrNull())

                // 2. Safely convert the URI to a File and wrap it in a MultipartBody.Part
                var imagePart: MultipartBody.Part? = null
                if (profilePhotoUri != null) {
                    val file = FileHelper.getFileFromUri(getApplication(), profilePhotoUri)
                    if (file != null) {
                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        // The string "profileImage" here MUST exactly match the Node.js multer configuration
                        imagePart = MultipartBody.Part.createFormData("profileImage", file.name, requestFile)
                    }
                }

                // 3. Send the Multipart request
                val response = api.signup(namePart, emailPart, passPart, imagePart)

                if (response.isSuccessful) {
                    pendingVerificationEmail = email
                    onOtpSent()
                } else {
                    _authError.value = parseError(response)
                }
            } catch (e: Exception) {
                _authError.value = "Network error: Make sure backend is running. ${e.localizedMessage}"
            } finally { _isLoading.value = false }
        }
    }
    fun verifyOtp(otp: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.verifyOtp(AuthRequest(email = pendingVerificationEmail, otp = otp))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    val account = SavedAccount(
                        accountId = body.user!!.accountId,
                        email = body.user.email,
                        name = body.user.name,
                        token = body.token!!,
                        profilePhotoUrl = formatImageUrl(body.user.profilePhotoUrl)
                    )


                    sessionManager.saveAccountAndSetActive(account)
                    onSuccess("Signup Complete: Email Verified!")
                } else {
                    _authError.value = parseError(response)
                }
            } catch (e: Exception) {
                _authError.value = "Network error: Make sure backend is running."
            } finally { _isLoading.value = false }
        }
    }

    fun resendSignupOtp(onResent: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Using forgotPassword as a universal OTP sender shortcut!
                val response = api.forgotPassword(AuthRequest(email = pendingVerificationEmail))
                if (response.isSuccessful) {
                    onResent()
                } else {
                    _authError.value = parseError(response)
                }
            } catch (e: Exception) {
                _authError.value = "Network error during resend."
            } finally { _isLoading.value = false }
        }
    }

    fun forgotPassword(email: String, onOtpSent: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.forgotPassword(AuthRequest(email = email))
                if (response.isSuccessful) {
                    resetEmail = email
                    onOtpSent()
                } else {
                    _authError.value = parseError(response)
                }
            } catch (e: Exception) {
                _authError.value = "Network error: Make sure backend is running."
            } finally { _isLoading.value = false }
        }
    }

    fun resetPassword(otp: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.resetPassword(AuthRequest(email = resetEmail, otp = otp, newPassword = newPassword))
                if (response.isSuccessful) onSuccess() else _authError.value = parseError(response)
            } catch (e: Exception) {
                _authError.value = "Network error."
            } finally { _isLoading.value = false }
        }
    }

    // --- ADD THIS HELPER FUNCTION ---
    private fun formatImageUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("http")) return url // Already an absolute URL

        // Ensure forward slashes (in case Node.js is running on Windows)
        val cleanPath = url.replace("\\", "/")

        // Grab the base URL (e.g., http://192.168.1.100:5001/api/auth/)
        // and strip the "api/auth/" part to get the pure server root.
        val serverRoot = com.example.healthx.BuildConfig.BASE_URL.replace("api/auth/", "")

        // Combine them safely
        val finalUrl = if (cleanPath.startsWith("/")) {
            serverRoot.dropLast(1) + cleanPath
        } else {
            serverRoot + cleanPath
        }

        Log.d("ProfileImageTracker", "Final Formatted URL saved to device: $finalUrl")
        return finalUrl
    }
}