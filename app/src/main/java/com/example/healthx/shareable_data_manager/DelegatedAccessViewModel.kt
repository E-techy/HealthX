package com.example.healthx.shareable_data_manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthx.data.network.RetrofitClient
import com.example.healthx.shareable_data_manager.data.CreateHashRequest
import com.example.healthx.shareable_data_manager.data.ErrorResponse
import com.example.healthx.shareable_data_manager.data.ShareableHash
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class DelegatedAccessViewModel : ViewModel() {
    private val api = RetrofitClient.delegatedAccessApi
    private val gson = Gson()

    private val _generateHashState = MutableStateFlow<UiState<ShareableHash>>(UiState.Idle)
    val generateHashState: StateFlow<UiState<ShareableHash>> = _generateHashState

    fun generateHash(token: String, selectedActions: List<String>) {
        if (selectedActions.isEmpty()) {
            _generateHashState.value = UiState.Error("Please select at least one permission.")
            return
        }

        _generateHashState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = api.createHash("Bearer $token", CreateHashRequest(selectedActions))
                if (response.isSuccessful && response.body()?.success == true) {
                    _generateHashState.value = UiState.Success(response.body()!!.data!!)
                } else {
                    _generateHashState.value = UiState.Error(parseError(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _generateHashState.value = UiState.Error(e.localizedMessage ?: "Network Error")
            }
        }
    }

    fun resetGenerateState() {
        _generateHashState.value = UiState.Idle
    }

    // Standardized Error Parser
    private fun parseError(errorBody: String?): String {
        return try {
            val errorRes = gson.fromJson(errorBody, ErrorResponse::class.java)
            errorRes.message
        } catch (e: Exception) {
            "An unexpected server error occurred."
        }
    }
}