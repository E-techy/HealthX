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
                        profilePhotoUrl = body.user.profilePhotoUrl // Safely capturing the URL from the backend
                    )
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
    fun signup(name: String, email: String, pass: String, profilePhotoUrl: String? = null, onOtpSent: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.signup(AuthRequest(email = email, password = pass, name = name, profilePhotoUrl = profilePhotoUrl))
                if (response.isSuccessful) {
                    pendingVerificationEmail = email
                    onOtpSent()
                } else {
                    _authError.value = parseError(response)
                }
            } catch (e: Exception) {
                _authError.value = "Network error: Make sure backend is running."
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
                        profilePhotoUrl = body.user.profilePhotoUrl // Safely capturing the URL from the backend
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
}