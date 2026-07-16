package com.example.healthx.shareable_data_manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthx.data.network.RetrofitClient
import com.example.healthx.shareable_data_manager.data.*
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

    val generateHashState = MutableStateFlow<UiState<ShareableHash>>(UiState.Idle)
    val myHashesState = MutableStateFlow<UiState<List<ShareableHash>>>(UiState.Idle)
    val blocklistState = MutableStateFlow<UiState<List<BlocklistedUser>>>(UiState.Idle)
    val receivedAccessState = MutableStateFlow<UiState<List<ReceivedAccessItem>>>(UiState.Idle)
    val grantedAccessState = MutableStateFlow<UiState<List<GrantedAccessItem>>>(UiState.Idle)

    // Quick action state (for blocking, updating, connecting)
    val actionState = MutableStateFlow<UiState<String>>(UiState.Idle)

    fun resetActionState() { actionState.value = UiState.Idle }
    fun resetGenerateState() { generateHashState.value = UiState.Idle }

    // --- Connecting ---
    fun connectWithHash(token: String, hashId: String) {
        actionState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = api.connectWithHash("Bearer $token", hashId)
                if (response.isSuccessful) {
                    actionState.value = UiState.Success("Successfully connected!")
                    fetchReceivedAccess(token) // Refresh list
                } else {
                    actionState.value = UiState.Error(parseError(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                actionState.value = UiState.Error("Network error while connecting.")
            }
        }
    }

    // --- Fetching Data ---
    fun fetchMyHashes(token: String) = fetchList(token, myHashesState) { api.getMyHashes("Bearer $it") }
    fun fetchBlocklist(token: String) = fetchList(token, blocklistState) { api.getBlocklist("Bearer $it") }
    fun fetchReceivedAccess(token: String) = fetchList(token, receivedAccessState) { api.getReceivedAccess("Bearer $it") }
    fun fetchGrantedAccess(token: String) = fetchList(token, grantedAccessState) { api.getGrantedAccess("Bearer $it") }

    private fun <T> fetchList(token: String, stateFlow: MutableStateFlow<UiState<List<T>>>, apiCall: suspend (String) -> retrofit2.Response<StandardResponse<List<T>>>) {
        stateFlow.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = apiCall(token)
                if (response.isSuccessful && response.body()?.success == true) {
                    stateFlow.value = UiState.Success(response.body()!!.data ?: emptyList())
                } else {
                    stateFlow.value = UiState.Error(parseError(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                stateFlow.value = UiState.Error("Network error loading data.")
            }
        }
    }

    // --- Managing Access & Permissions ---
    fun updatePermissions(token: String, targetUserId: String, permissions: List<ActivePermission>) {
        actionState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = api.updateFriendPermissions("Bearer $token", targetUserId, UpdatePermissionsRequest(permissions))
                if (response.isSuccessful) {
                    actionState.value = UiState.Success("Permissions updated successfully.")
                    fetchGrantedAccess(token)
                } else actionState.value = UiState.Error(parseError(response.errorBody()?.string()))
            } catch (e: Exception) { actionState.value = UiState.Error("Network error updating permissions.") }
        }
    }

    fun blockUser(token: String, targetUserId: String, reason: String, notes: String) {
        actionState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = api.blockUser("Bearer $token", targetUserId, BlockUserRequest(reason, notes))
                if (response.isSuccessful) {
                    actionState.value = UiState.Success("User blocked and access revoked.")
                    fetchGrantedAccess(token)
                    fetchReceivedAccess(token)
                } else actionState.value = UiState.Error(parseError(response.errorBody()?.string()))
            } catch (e: Exception) { actionState.value = UiState.Error("Network error blocking user.") }
        }
    }

    // ... Hash generation/toggling/deleting functions stay the same as previous response ...
    fun generateHash(token: String, selectedActions: List<String>) {
        if (selectedActions.isEmpty()) { generateHashState.value = UiState.Error("Select at least one permission."); return }
        generateHashState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = api.createHash("Bearer $token", CreateHashRequest(selectedActions))
                if (response.isSuccessful) generateHashState.value = UiState.Success(response.body()!!.data!!)
                else generateHashState.value = UiState.Error(parseError(response.errorBody()?.string()))
            } catch (e: Exception) { generateHashState.value = UiState.Error("Network error.") }
        }
    }

    fun toggleHashStatus(token: String, hashId: String, currentStatus: String) {
        viewModelScope.launch {
            val newStatus = if (currentStatus == "ACTIVE") "UNACTIVE" else "ACTIVE"
            try {
                if (api.updateHashStatus("Bearer $token", hashId, UpdateHashStatusRequest(newStatus)).isSuccessful) fetchMyHashes(token)
            } catch (e: Exception) {}
        }
    }

    fun deleteHash(token: String, hashId: String) {
        viewModelScope.launch {
            try {
                if (api.deleteHash("Bearer $token", hashId).isSuccessful) fetchMyHashes(token)
            } catch (e: Exception) {}
        }
    }

    fun unblockUser(token: String, blockedUserId: String) {
        viewModelScope.launch {
            try {
                if (api.unblockUser("Bearer $token", blockedUserId).isSuccessful) fetchBlocklist(token)
            } catch (e: Exception) {}
        }
    }

    private fun parseError(errorBody: String?): String {
        return try { gson.fromJson(errorBody, ErrorResponse::class.java).message }
        catch (e: Exception) { "An unexpected server error occurred." }
    }
}