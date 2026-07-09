package com.example.healthx.ui.screens.settings

import android.app.Application
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

    private val _settingsData = MutableStateFlow(UserSettingsData())
    val settingsData: StateFlow<UserSettingsData> = _settingsData

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState

    // Available AI Models map definition
    val aiProviders = listOf("Google", "OpenAI")
    val modelsMap = mapOf(
        "Google" to listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-1.5-flash"),
        "OpenAI" to listOf("gpt-4o", "gpt-4o-mini", "o1-mini")
    )

    fun fetchSettings() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val token = getAuthToken()
            if (token == null) {
                _uiState.value = SettingsUiState.Error("Session token missing. Re-authenticate.")
                return@launch
            }

            try {
                val response = RetrofitClient.settingsApi.getSettings(token)
                if (response.isSuccessful && response.body()?.success == true) {
                    _settingsData.value = response.body()!!.data
                    _uiState.value = SettingsUiState.Idle
                } else {
                    _uiState.value = SettingsUiState.Error(response.message() ?: "Failed to get settings")
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.localizedMessage ?: "Network error occurred")
            }
        }
    }

    fun updateSettings(updatedData: UserSettingsData) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val token = getAuthToken()
            if (token == null) {
                _uiState.value = SettingsUiState.Error("Session missing.")
                return@launch
            }

            try {
                val response = RetrofitClient.settingsApi.updateSettings(token, updatedData)
                if (response.isSuccessful && response.body()?.success == true) {
                    _settingsData.value = response.body()!!.data
                    _uiState.value = SettingsUiState.Success("Settings updated successfully")
                } else {
                    _uiState.value = SettingsUiState.Error(response.message() ?: "Failed to update settings")
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.localizedMessage ?: "Network error")
            }
        }
    }

    // Mutation helpers for Lists
    fun addAllergy(allergy: String) {
        if (allergy.isBlank()) return
        val clean = allergy.trim()
        if (!_settingsData.value.allergies.contains(clean)) {
            _settingsData.value = _settingsData.value.copy(
                allergies = _settingsData.value.allergies + clean
            )
        }
    }

    fun removeAllergy(allergy: String) {
        _settingsData.value = _settingsData.value.copy(
            allergies = _settingsData.value.allergies - allergy
        )
    }

    fun addApiKey(key: ApiKeyItem) {
        // filter out existing matching pair configuration if user updates it
        val filtered = _settingsData.value.apiKeys.filterNot {
            it.companyName == key.companyName && it.modelName == key.modelName
        }
        _settingsData.value = _settingsData.value.copy(
            apiKeys = filtered + key
        )
    }

    fun removeApiKey(key: ApiKeyItem) {
        _settingsData.value = _settingsData.value.copy(
            apiKeys = _settingsData.value.apiKeys - key
        )
    }

    private suspend fun getAuthToken(): String? {
        val account = sessionManager.activeAccountFlow.firstOrNull() ?: return null
        // Ensure "Bearer " format prefix matches authMiddleware verification logic
        return "Bearer ${account.token}"
    }

    fun resetUiState() {
        _uiState.value = SettingsUiState.Idle
    }
}