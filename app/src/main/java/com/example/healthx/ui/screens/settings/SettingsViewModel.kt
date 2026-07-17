package com.example.healthx.ui.screens.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthx.data.local.SessionManager
import com.example.healthx.data.network.ApiKeyItem
import com.example.healthx.data.network.RetrofitClient
import com.example.healthx.data.network.UserSettingsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed class SettingsUiState {
    object Idle : SettingsUiState()
    object Loading : SettingsUiState()
    data class Success(val message: String) : SettingsUiState()
    data class Error(val exception: String) : SettingsUiState()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val TAG = "HealthX_SettingsVM"

    private val _settingsData = MutableStateFlow(UserSettingsData())
    val settingsData: StateFlow<UserSettingsData> = _settingsData

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState

    // Static collections for advanced dropdown parameters
    val ethnicityOptions = listOf("South Asian", "East Asian", "Caucasian", "African American", "Hispanic", "Other")
    val countryOptions = listOf("India", "United States", "United Kingdom", "Canada", "Australia", "Other")
    val languageOptions = listOf("English", "Hindi", "Spanish", "French", "German")

    val aiProviders = listOf("Google", "OpenAI")
    val modelsMap = mapOf(
        "Google" to listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-1.5-flash"),
        "OpenAI" to listOf("gpt-4o", "gpt-4o-mini", "o1-mini")
    )

    fun fetchSettings() {
        viewModelScope.launch {
            Log.d(TAG, "🔄 Initiating fetchSettings() call...")
            _uiState.value = SettingsUiState.Loading

            val token = getAuthToken()
            if (token == null) {
                Log.e(TAG, "❌ Fetch failed: Active Session Token is missing or NULL")
                _uiState.value = SettingsUiState.Error("Session token missing. Re-authenticate.")
                return@launch
            }

            try {
                Log.d(TAG, "📡 Sending GET request to /api/settings...")
                val response = RetrofitClient.settingsApi.getSettings(token)

                if (response.isSuccessful && response.body()?.success == true) {
                    val receivedData = response.body()!!.data
                    Log.d(TAG, "✅ Fetch Success! Received Server Payload: $receivedData")
                    _settingsData.value = receivedData
                    _uiState.value = SettingsUiState.Idle
                } else {
                    val rawError = response.errorBody()?.string()
                    Log.e(TAG, "❌ Server Rejected Fetch Request! Code: ${response.code()}, ErrorBody: $rawError")
                    _uiState.value = SettingsUiState.Error("Server returned error ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Critical exception during settings fetching!", e)
                _uiState.value = SettingsUiState.Error(e.localizedMessage ?: "Network connection error")
            }
        }
    }

    fun updateSettings(updatedData: UserSettingsData) {
        viewModelScope.launch {
            Log.d(TAG, "💾 Initiating updateSettings() process...")
            Log.d(TAG, "📤 Payload details targeted for transmission: $updatedData")
            _uiState.value = SettingsUiState.Loading

            val token = getAuthToken()
            if (token == null) {
                Log.e(TAG, "❌ Update operation aborted: Session Token is unavailable")
                _uiState.value = SettingsUiState.Error("Authorization credentials lost.")
                return@launch
            }

            try {
                Log.d(TAG, "📡 Transmitting PUT payload structure to server...")
                val response = RetrofitClient.settingsApi.updateSettings(token, updatedData)

                if (response.isSuccessful && response.body()?.success == true) {
                    val savedData = response.body()!!.data
                    Log.d(TAG, "✅ Update Confirmed! Returned Server Payload confirmation: $savedData")

                    _settingsData.value = savedData
                    _uiState.value = SettingsUiState.Success("Settings updated successfully")
                } else {
                    val rawError = response.errorBody()?.string()
                    Log.e(TAG, "❌ Server Update Refusal! Status Code: ${response.code()}, Body: $rawError")
                    _uiState.value = SettingsUiState.Error("Save failed: Code ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Critical connectivity exception during update transmission!", e)
                _uiState.value = SettingsUiState.Error(e.localizedMessage ?: "Failed to reach server")
            }
        }
    }

    fun addAllergy(allergy: String) {
        val clean = allergy.trim()
        if (clean.isNotBlank() && !_settingsData.value.allergies.contains(clean)) {
            Log.d(TAG, "➕ Local Allergy Append: $clean")
            _settingsData.value = _settingsData.value.copy(allergies = _settingsData.value.allergies + clean)
        }
    }

    fun removeAllergy(allergy: String) {
        Log.d(TAG, "➖ Local Allergy Detach: $allergy")
        _settingsData.value = _settingsData.value.copy(allergies = _settingsData.value.allergies - allergy)
    }

    fun addApiKey(key: ApiKeyItem) {
        Log.d(TAG, "➕ Local API Matrix Registration/Update for Provider: ${key.companyName}")
        val filtered = _settingsData.value.apiKeys.filterNot { it.companyName == key.companyName && it.modelName == key.modelName }
        _settingsData.value = _settingsData.value.copy(apiKeys = filtered + key)
    }

    fun removeApiKey(key: ApiKeyItem) {
        Log.d(TAG, "➖ Local API Key Reference Removal: ${key.companyName} -> ${key.modelName}")
        _settingsData.value = _settingsData.value.copy(apiKeys = _settingsData.value.apiKeys - key)
    }

    private suspend fun getAuthToken(): String? {
        val account = sessionManager.activeAccountFlow.firstOrNull() ?: return null
        return "Bearer ${account.token}"
    }

    fun resetUiState() {
        _uiState.value = SettingsUiState.Idle
    }
}