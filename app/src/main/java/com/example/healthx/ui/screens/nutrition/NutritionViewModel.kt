package com.example.healthx.ui.screens.nutrition

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthx.data.local.SessionManager
import com.example.healthx.data.models.*
import com.example.healthx.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

// Tag for Logcat
private const val TAG = "NutritionViewModel"

sealed class NutritionState {
    object Loading : NutritionState()
    data class Dashboard(val summary: DailySummary) : NutritionState()
    data class AiReview(val analysisData: AiFoodData) : NutritionState()
    data class Error(val message: String) : NutritionState()
}

class NutritionViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<NutritionState>(NutritionState.Loading)
    val uiState = _uiState.asStateFlow()

    private val sessionManager = SessionManager(application)
    private var jwtToken: String = ""

    init {
        Log.d(TAG, "ViewModel initialized. Collecting session data...")
        viewModelScope.launch {
            sessionManager.activeAccountFlow.collect { account ->
                if (account != null) {
                    jwtToken = "Bearer ${account.token}"
                    Log.d(TAG, "Session found. Token loaded. Fetching dashboard.")
                    fetchDashboard()
                } else {
                    Log.e(TAG, "No active session found. Emitting Auth Error.")
                    _uiState.value = NutritionState.Error("Authentication Error. Please log in again.")
                }
            }
        }
    }

    fun fetchDashboard() {
        if (jwtToken.isEmpty()) {
            Log.w(TAG, "fetchDashboard aborted: jwtToken is empty.")
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "--> GET /api/nutrition/today")
            _uiState.value = NutritionState.Loading
            try {
                val response = RetrofitClient.nutritionApi.getTodayDashboard(jwtToken)
                if (response.success) {
                    Log.d(TAG, "<-- Dashboard fetched successfully: ${response.summary}")
                    _uiState.value = NutritionState.Dashboard(response.summary)
                } else {
                    Log.e(TAG, "<-- Server returned success=false without HTTP error.")
                    _uiState.value = NutritionState.Error("Failed to load dashboard data.")
                }
            } catch (e: Exception) {
                val errorMsg = parseErrorMessage(e)
                Log.e(TAG, "fetchDashboard failed: $errorMsg", e)
                _uiState.value = NutritionState.Error(errorMsg)
            }
        }
    }

    fun analyzeImage(context: Context, uri: Uri, portionSize: Int) {
        if (jwtToken.isEmpty()) {
            Log.w(TAG, "analyzeImage aborted: jwtToken is empty.")
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "--> POST /api/nutrition/ai/analyze | Portion: $portionSize")
            _uiState.value = NutritionState.Loading
            try {
                val file = getFileFromUri(context, uri)
                Log.d(TAG, "Image file prepared: ${file.name} (${file.length()} bytes)")

                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                val portionBody = portionSize.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                val response = RetrofitClient.nutritionApi.analyzeFoodImage(jwtToken, imagePart, portionBody)

                if (response.success) {
                    Log.d(TAG, "<-- AI Analysis Successful: ${response.data.foodDetected}")
                    _uiState.value = NutritionState.AiReview(response.data)
                } else {
                    Log.e(TAG, "<-- AI Analysis returned success=false")
                    _uiState.value = NutritionState.Error("AI Analysis failed")
                }
            } catch (e: Exception) {
                val errorMsg = parseErrorMessage(e)
                Log.e(TAG, "analyzeImage failed: $errorMsg", e)
                _uiState.value = NutritionState.Error(errorMsg)
            }
        }
    }

    fun consumeAndTrackMeal(data: AiFoodData) {
        if (jwtToken.isEmpty()) return

        viewModelScope.launch {
            Log.d(TAG, "--> POST /api/nutrition/ai/save | Food: ${data.foodDetected}")
            _uiState.value = NutritionState.Loading
            try {
                val request = SaveAiMealRequest(
                    foodName = data.foodDetected,
                    imageUrl = data.imageUrl,
                    foodQualityScore = data.scores.foodQualityScore,
                    aiInsights = data.aiInsights,
                    portionAnalyzed = data.portionAnalyzed,
                    rawNutrients = data.rawNutrientsExtracted,
                    allergens = data.allergens
                )

                val response = RetrofitClient.nutritionApi.saveAiMeal(jwtToken, request)
                if (response.success) {
                    Log.d(TAG, "<-- Meal saved successfully. Reloading dashboard.")
                    fetchDashboard()
                } else {
                    Log.e(TAG, "<-- Meal save returned success=false")
                    _uiState.value = NutritionState.Error("Failed to save meal data.")
                }
            } catch (e: Exception) {
                val errorMsg = parseErrorMessage(e)
                Log.e(TAG, "consumeAndTrackMeal failed: $errorMsg", e)
                _uiState.value = NutritionState.Error(errorMsg)
            }
        }
    }

    fun cancelAiReview() {
        Log.d(TAG, "User cancelled AI review. Reloading dashboard.")
        fetchDashboard()
    }

    private fun getFileFromUri(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(tempFile)

        try {
            // Decode the image stream into a Bitmap
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Compress the bitmap directly to the output stream as a JPEG at 70% quality
            // This will turn a 5MB image into a ~500KB image without losing AI-readable detail
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Image compression failed, falling back to raw copy", e)
            // Fallback just in case
            inputStream?.copyTo(outputStream)
        } finally {
            inputStream?.close()
            outputStream.flush()
            outputStream.close()
        }

        Log.d(TAG, "Compressed file size: ${tempFile.length() / 1024} KB")
        return tempFile
    }

    // ==========================================
    // THE MAGIC ERROR PARSER YOU NEEDED
    // ==========================================
    private fun parseErrorMessage(e: Exception): String {
        if (e is HttpException) {
            try {
                val errorBodyString = e.response()?.errorBody()?.string()
                if (!errorBodyString.isNullOrEmpty()) {
                    val jsonObject = JSONObject(errorBodyString)
                    // Look for the "message" key sent by the Node server
                    if (jsonObject.has("message")) {
                        return jsonObject.getString("message")
                    }
                }
            } catch (parseException: Exception) {
                Log.e(TAG, "Failed to parse error body JSON", parseException)
            }
            // Fallback if we couldn't parse the JSON
            return "Server Error: ${e.code()}"
        }
        return e.message ?: "An unexpected network error occurred."
    }
}