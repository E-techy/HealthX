package com.example.healthx.ui.screens.scanner

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class ScanDataType { URL, JSON, TEXT }
enum class ItemStatus { PENDING, LOADING, IMPORTED, ADDED }

data class ScannedItem(
    val id: Int,
    val type: ScanDataType,
    val rawData: String,
    val displayData: String,
    val status: ItemStatus
)

class QRScannerViewModel(application: Application) : AndroidViewModel(application) {

    // List of items added to the queue below the scanner
    private val _scannedItems = MutableStateFlow<List<ScannedItem>>(emptyList())
    val scannedItems = _scannedItems.asStateFlow()

    // The current item currently detected in the camera (but not yet added to the list)
    private val _currentPendingScan = MutableStateFlow<String?>(null)
    val currentPendingScan = _currentPendingScan.asStateFlow()

    private val _currentScanType = MutableStateFlow(ScanDataType.TEXT)
    val currentScanType = _currentScanType.asStateFlow()

    private var itemIdCounter = 1

    /**
     * Called by the camera analyzer or gallery picker when a barcode is found.
     */
    fun processScannedText(text: String) {
        // Prevent flickering if it's the same QR code currently pending
        if (_currentPendingScan.value == text) return

        _currentPendingScan.value = text
        _currentScanType.value = classifyData(text)
    }

    /**
     * Classifies whether the string is a URL, JSON, or plain text.
     */
    private fun classifyData(text: String): ScanDataType {
        if (Patterns.WEB_URL.matcher(text).matches()) {
            return ScanDataType.URL
        }
        try {
            // Try parsing as JSON Object or Array
            if (text.trim().startsWith("{")) {
                JSONObject(text)
                return ScanDataType.JSON
            } else if (text.trim().startsWith("[")) {
                JSONArray(text)
                return ScanDataType.JSON
            }
        } catch (e: Exception) {
            // Not valid JSON
        }
        return ScanDataType.TEXT
    }

    /**
     * Called when the user clicks "Import" (for URLs) or "Add" (for JSON/Text)
     */
    fun onImportOrAddClicked() {
        val rawText = _currentPendingScan.value ?: return
        val type = _currentScanType.value

        val newItem = ScannedItem(
            id = itemIdCounter++,
            type = type,
            rawData = rawText,
            displayData = formatData(rawText, type),
            status = if (type == ScanDataType.URL) ItemStatus.LOADING else ItemStatus.ADDED
        )

        // Add to the list
        _scannedItems.value = _scannedItems.value + newItem

        // Clear the pending scan overlay
        _currentPendingScan.value = null

        // If it's a URL, simulate a download process
        if (type == ScanDataType.URL) {
            simulateDownload(newItem.id)
        }
    }

    private fun simulateDownload(itemId: Int) {
        viewModelScope.launch {
            delay(2000) // Simulate network delay
            _scannedItems.value = _scannedItems.value.map { item ->
                if (item.id == itemId) item.copy(status = ItemStatus.IMPORTED) else item
            }
        }
    }

    /**
     * A dummy formatter script for JSON payloads.
     */
    private fun formatData(text: String, type: ScanDataType): String {
        return if (type == ScanDataType.JSON) {
            try {
                // Prettify the JSON for the UI
                JSONObject(text).toString(4)
            } catch (e: Exception) {
                text
            }
        } else {
            text
        }
    }

    fun deleteItem(id: Int) {
        _scannedItems.value = _scannedItems.value.filter { it.id != id }
    }

    fun clearPendingScan() {
        _currentPendingScan.value = null
    }

    fun saveAllItems() {
        // TODO: Send data to Reminders Screen or local database
        _scannedItems.value = emptyList() // Clear the list after saving
        itemIdCounter = 1
    }
}