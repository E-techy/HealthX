package com.example.healthx.shareable_data_manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthx.data.network.RetrofitClient
import com.example.healthx.shareable_data_manager.data.BlocklistedUser
import com.example.healthx.shareable_data_manager.data.CreateHashRequest
import com.example.healthx.shareable_data_manager.data.ErrorResponse
import com.example.healthx.shareable_data_manager.data.ShareableHash
import com.example.healthx.shareable_data_manager.data.UpdateHashStatusRequest
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.healthx.shareable_data_manager.data.ReceivedAccessItem

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class DelegatedAccessViewModel : ViewModel() {
    private val api = RetrofitClient.delegatedAccessApi
    private val gson = Gson()

    // Generate Hash State
    private val _generateHashState = MutableStateFlow<UiState<ShareableHash>>(UiState.Idle)
    val generateHashState: StateFlow<UiState<ShareableHash>> = _generateHashState

    // My Hashes State
    private val _myHashesState = MutableStateFlow<UiState<List<ShareableHash>>>(UiState.Idle)
    val myHashesState: StateFlow<UiState<List<ShareableHash>>> = _myHashesState

    // Blocklist State
    private val _blocklistState = MutableStateFlow<UiState<List<BlocklistedUser>>>(UiState.Idle)
    val blocklistState: StateFlow<UiState<List<BlocklistedUser>>> = _blocklistState

    private val _receivedAccessState = MutableStateFlow<UiState<List<ReceivedAccessItem>>>(UiState.Idle)
    val receivedAccessState: StateFlow<UiState<List<ReceivedAccessItem>>> = _receivedAccessState


    fun fetchReceivedAccess(token: String) {
        _receivedAccessState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = api.getReceivedAccess("Bearer $token")
                if (response.isSuccessful && response.body()?.success == true) {
                    _receivedAccessState.value = UiState.Success(response.body()!!.data ?: emptyList())
                } else {
                    _receivedAccessState.value = UiState.Error(parseError(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _receivedAccessState.value = UiState.Error("Failed to load accessible profiles.")
            }
        }
    }

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
                _generateHashState.value = UiState.Error("Network Error: Could not connect to server.")
            }
        }
    }

    fun resetGenerateState() {
        _generateHashState.value = UiState.Idle
    }

    // --- Hash Management ---

    fun fetchMyHashes(token: String) {
        _myHashesState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = api.getMyHashes("Bearer $token")
                if (response.isSuccessful && response.body()?.success == true) {
                    _myHashesState.value = UiState.Success(response.body()!!.data ?: emptyList())
                } else {
                    _myHashesState.value = UiState.Error(parseError(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _myHashesState.value = UiState.Error("Failed to load your shared links.")
            }
        }
    }

    fun toggleHashStatus(token: String, hashId: String, currentStatus: String) {
        viewModelScope.launch {
            val newStatus = if (currentStatus == "ACTIVE") "UNACTIVE" else "ACTIVE"
            try {
                val response = api.updateHashStatus("Bearer $token", hashId, UpdateHashStatusRequest(newStatus))
                if (response.isSuccessful) {
                    fetchMyHashes(token) // Refresh list on success
                } else {
                    _myHashesState.value = UiState.Error(parseError(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _myHashesState.value = UiState.Error("Network error while updating status.")
            }
        }
    }

    fun deleteHash(token: String, hashId: String) {
        viewModelScope.launch {
            try {
                val response = api.deleteHash("Bearer $token", hashId)
                if (response.isSuccessful) {
                    fetchMyHashes(token) // Refresh list
                } else {
                    _myHashesState.value = UiState.Error(parseError(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _myHashesState.value = UiState.Error("Network error while deleting link.")
            }
        }
    }

    // --- Blocklist Management ---

    fun fetchBlocklist(token: String) {
        _blocklistState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = api.getBlocklist("Bearer $token")
                if (response.isSuccessful && response.body()?.success == true) {
                    _blocklistState.value = UiState.Success(response.body()!!.data ?: emptyList())
                } else {
                    _blocklistState.value = UiState.Error(parseError(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _blocklistState.value = UiState.Error("Failed to load blocklist.")
            }
        }
    }

    fun unblockUser(token: String, blockedUserId: String) {
        viewModelScope.launch {
            try {
                val response = api.unblockUser("Bearer $token", blockedUserId)
                if (response.isSuccessful) {
                    fetchBlocklist(token) // Refresh list
                } else {
                    _blocklistState.value = UiState.Error(parseError(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _blocklistState.value = UiState.Error("Network error while unblocking user.")
            }
        }
    }

    private fun parseError(errorBody: String?): String {
        return try {
            val errorRes = gson.fromJson(errorBody, ErrorResponse::class.java)
            errorRes.message
        } catch (e: Exception) {
            "An unexpected server error occurred."
        }
    }
}