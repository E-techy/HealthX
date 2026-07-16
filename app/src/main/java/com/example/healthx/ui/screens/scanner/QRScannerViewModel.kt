package com.example.healthx.ui.screens.scanner

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthx.data.local.SessionManager
import com.example.healthx.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

enum class ScanDataType {
    TEXT, URL, JSON, SHARE_ACCESS // Added SHARE_ACCESS
}

enum class ItemStatus {
    SCANNED, LOADING, IMPORTED, ADDED
}

data class ScannedItem(
    val id: String = UUID.randomUUID().toString(),
    val rawData: String,
    val displayData: String,
    val type: ScanDataType,
    var status: ItemStatus = ItemStatus.SCANNED
)

class QRScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val delegatedApi = RetrofitClient.delegatedAccessApi

    private val _scannedItems = MutableStateFlow<List<ScannedItem>>(emptyList())
    val scannedItems: StateFlow<List<ScannedItem>> = _scannedItems.asStateFlow()

    private val _currentPendingScan = MutableStateFlow<String?>(null)
    val currentPendingScan: StateFlow<String?> = _currentPendingScan.asStateFlow()

    private val _currentScanType = MutableStateFlow<ScanDataType>(ScanDataType.TEXT)
    val currentScanType: StateFlow<ScanDataType> = _currentScanType.asStateFlow()

    // Status message for the UI (Success/Error toasts)
    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    fun clearMessage() { _scanMessage.value = null }

    fun processScannedText(text: String) {
        // Prevent re-processing the exact same code rapidly
        if (text == _currentPendingScan.value) return

        // 1. Check for Delegated Access Hash
        try {
            val jsonObj = JSONObject(text)
            if (jsonObj.has("category") && jsonObj.getString("category") == "SHARE_ACCESS") {
                val hashId = jsonObj.getString("hash")
                _currentPendingScan.value = hashId
                _currentScanType.value = ScanDataType.SHARE_ACCESS
                return
            }
        } catch (e: Exception) { /* Not a valid JSON or not our specific format, proceed normally */ }

        // 2. Normal processing (URL, JSON, TEXT)
        _currentPendingScan.value = text
        _currentScanType.value = when {
            text.startsWith("http://") || text.startsWith("https://") -> ScanDataType.URL
            text.startsWith("{") || text.startsWith("[") -> ScanDataType.JSON
            else -> ScanDataType.TEXT
        }
    }

    fun onActionClicked() {
        val data = _currentPendingScan.value ?: return
        val type = _currentScanType.value

        if (type == ScanDataType.SHARE_ACCESS) {
            connectWithFriendHash(data)
            // Clear the overlay immediately after clicking
            _currentPendingScan.value = null
        } else {
            // Add normal scanned item to the list
            val newItem = ScannedItem(
                rawData = data,
                displayData = if (data.length > 50) data.take(50) + "..." else data,
                type = type
            )
            _scannedItems.value = _scannedItems.value + newItem
            _currentPendingScan.value = null // Clear overlay
        }
    }

    private fun connectWithFriendHash(hashId: String) {
        viewModelScope.launch {
            val token = sessionManager.activeAccountFlow.firstOrNull()?.token
            if (token == null) {
                _scanMessage.value = "Authentication error. Please log in again."
                return@launch
            }

            try {
                val response = delegatedApi.connectWithHash("Bearer $token", hashId)
                if (response.isSuccessful && response.body()?.success == true) {
                    _scanMessage.value = "Success! You are now connected."
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = try {
                        JSONObject(errorBody!!).getString("message")
                    } catch (e: Exception) { "Failed to connect." }
                    _scanMessage.value = errorMsg
                }
            } catch (e: Exception) {
                _scanMessage.value = "Network error while connecting."
                Log.e("QRScannerViewModel", "Connect error", e)
            }
        }
    }

    fun deleteItem(id: String) {
        _scannedItems.value = _scannedItems.value.filter { it.id != id }
    }

    fun saveAllItems() {
        // Implementation for saving normal text/URL/JSON items locally
        _scannedItems.value = emptyList()
    }

    fun clearPendingScan() {
        _currentPendingScan.value = null
    }
}