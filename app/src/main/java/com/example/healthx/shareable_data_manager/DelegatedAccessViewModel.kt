package com.example.healthx.shareable_data_manager

import android.util.Log
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
    private val TAG = "DelegatedAccessVM"

    val generateHashState = MutableStateFlow<UiState<ShareableHash>>(UiState.Idle)
    val myHashesState = MutableStateFlow<UiState<List<ShareableHash>>>(UiState.Idle)
    val blocklistState = MutableStateFlow<UiState<List<BlocklistedUser>>>(UiState.Idle)
    val receivedAccessState = MutableStateFlow<UiState<List<ReceivedAccessItem>>>(UiState.Idle)
    val grantedAccessState = MutableStateFlow<UiState<List<GrantedAccessItem>>>(UiState.Idle)

    val actionState = MutableStateFlow<UiState<String>>(UiState.Idle)

    fun resetActionState() { actionState.value = UiState.Idle }
    fun resetGenerateState() { generateHashState.value = UiState.Idle }

    // --- Connecting ---
    fun connectWithHash(token: String, hashId: String) {
        Log.d(TAG, "🔗 Attempting to connect with hash: $hashId")
        actionState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = api.connectWithHash("Bearer $token", hashId)
                if (response.isSuccessful) {
                    Log.d(TAG, "✅ Successfully connected using hash.")
                    actionState.value = UiState.Success("Successfully connected!")
                    fetchReceivedAccess(token)
                } else {
                    val errorMsg = parseError(response.errorBody()?.string())
                    Log.e(TAG, "❌ Failed to connect: $errorMsg")
                    actionState.value = UiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Network error during connect: ${e.message}", e)
                actionState.value = UiState.Error("Network error while connecting.")
            }
        }
    }

    // --- Fetching Data ---
    fun fetchMyHashes(token: String) {
        Log.d(TAG, "📡 Fetching generated hashes...")
        fetchList(token, myHashesState) { api.getMyHashes("Bearer $it") }
    }
    fun fetchBlocklist(token: String) {
        Log.d(TAG, "📡 Fetching blocklist...")
        fetchList(token, blocklistState) { api.getBlocklist("Bearer $it") }
    }
    fun fetchReceivedAccess(token: String) {
        Log.d(TAG, "📡 Fetching Profiles I Can View (Received Access)...")
        fetchList(token, receivedAccessState) { api.getReceivedAccess("Bearer $it") }
    }
    fun fetchGrantedAccess(token: String) {
        Log.d(TAG, "📡 Fetching Who Has Access (Granted Access)...")
        fetchList(token, grantedAccessState) { api.getGrantedAccess("Bearer $it") }
    }

    private fun <T> fetchList(token: String, stateFlow: MutableStateFlow<UiState<List<T>>>, apiCall: suspend (String) -> retrofit2.Response<StandardResponse<List<T>>>) {
        stateFlow.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = apiCall(token)
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()!!.data ?: emptyList()
                    Log.d(TAG, "✅ Fetch successful. Received ${data.size} items.")
                    stateFlow.value = UiState.Success(data)
                } else {
                    val errorMsg = parseError(response.errorBody()?.string())
                    Log.e(TAG, "❌ Fetch failed: $errorMsg")
                    stateFlow.value = UiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Network error fetching list: ${e.message}", e)
                stateFlow.value = UiState.Error("Network error loading data.")
            }
        }
    }

    // --- Managing Access & Permissions ---
    fun updatePermissions(token: String, targetUserId: String, permissions: List<ActivePermission>) {
        Log.d(TAG, "📝 Updating permissions for user: $targetUserId")
        actionState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = api.updateFriendPermissions("Bearer $token", targetUserId, UpdatePermissionsRequest(permissions))
                if (response.isSuccessful) {
                    Log.d(TAG, "✅ Permissions updated successfully.")
                    actionState.value = UiState.Success("Permissions updated successfully.")
                    fetchGrantedAccess(token)
                } else {
                    val errorMsg = parseError(response.errorBody()?.string())
                    Log.e(TAG, "❌ Failed to update permissions: $errorMsg")
                    actionState.value = UiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Network error updating permissions: ${e.message}", e)
                actionState.value = UiState.Error("Network error updating permissions.")
            }
        }
    }

    fun blockUser(token: String, targetUserId: String, reason: String, notes: String) {
        Log.d(TAG, "🛑 Blocking user: $targetUserId | Reason: $reason | Notes: $notes")
        actionState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = api.blockUser("Bearer $token", targetUserId, BlockUserRequest(reason, notes))
                if (response.isSuccessful) {
                    Log.d(TAG, "✅ User blocked successfully.")
                    actionState.value = UiState.Success("User blocked and access revoked.")
                    fetchGrantedAccess(token)
                    fetchReceivedAccess(token)
                } else {
                    val errorMsg = parseError(response.errorBody()?.string())
                    Log.e(TAG, "❌ Failed to block user: $errorMsg")
                    actionState.value = UiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Network error blocking user: ${e.message}", e)
                actionState.value = UiState.Error("Network error blocking user.")
            }
        }
    }

    fun generateHash(token: String, selectedActions: List<String>) {
        Log.d(TAG, "🖨️ Generating new QR Hash for actions: $selectedActions")
        if (selectedActions.isEmpty()) {
            generateHashState.value = UiState.Error("Select at least one permission.")
            return
        }
        generateHashState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = api.createHash("Bearer $token", CreateHashRequest(selectedActions))
                if (response.isSuccessful) {
                    Log.d(TAG, "✅ QR Hash generated successfully.")
                    generateHashState.value = UiState.Success(response.body()!!.data!!)
                } else {
                    val errorMsg = parseError(response.errorBody()?.string())
                    Log.e(TAG, "❌ Failed to generate hash: $errorMsg")
                    generateHashState.value = UiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Network error generating hash: ${e.message}", e)
                generateHashState.value = UiState.Error("Network error.")
            }
        }
    }

    fun toggleHashStatus(token: String, hashId: String, currentStatus: String) {
        Log.d(TAG, "🔄 Toggling hash status for $hashId. Current: $currentStatus")
        viewModelScope.launch {
            val newStatus = if (currentStatus == "ACTIVE") "UNACTIVE" else "ACTIVE"
            try {
                if (api.updateHashStatus("Bearer $token", hashId, UpdateHashStatusRequest(newStatus)).isSuccessful) fetchMyHashes(token)
            } catch (e: Exception) { Log.e(TAG, "💥 Error toggling hash status.", e) }
        }
    }

    fun deleteHash(token: String, hashId: String) {
        Log.d(TAG, "🗑️ Deleting hash: $hashId")
        viewModelScope.launch {
            try {
                if (api.deleteHash("Bearer $token", hashId).isSuccessful) fetchMyHashes(token)
            } catch (e: Exception) { Log.e(TAG, "💥 Error deleting hash.", e) }
        }
    }

    fun unblockUser(token: String, blockedUserId: String) {
        Log.d(TAG, "🔓 Unblocking user: $blockedUserId")
        viewModelScope.launch {
            try {
                if (api.unblockUser("Bearer $token", blockedUserId).isSuccessful) fetchBlocklist(token)
            } catch (e: Exception) { Log.e(TAG, "💥 Error unblocking user.", e) }
        }
    }

    private fun parseError(errorBody: String?): String {
        return try { gson.fromJson(errorBody, ErrorResponse::class.java).message }
        catch (e: Exception) { "An unexpected server error occurred." }
    }
}