package com.example.healthx.ui.screens.nutrition

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthx.data.models.*
import com.example.healthx.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

sealed class NutritionState {
    object Loading : NutritionState()
    data class Dashboard(val summary: DailySummary) : NutritionState()
    data class AiReview(val analysisData: AiFoodData) : NutritionState()
    data class Error(val message: String) : NutritionState()
}

class NutritionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<NutritionState>(NutritionState.Loading)
    val uiState = _uiState.asStateFlow()

    // TODO: Inject your Auth Manager to get the real JWT token
    private val jwtToken = "Bearer YOUR_JWT_TOKEN_HERE"

    init {
        fetchDashboard()
    }

    fun fetchDashboard() {
        viewModelScope.launch {
            _uiState.value = NutritionState.Loading
            try {
                val response = RetrofitClient.nutritionApi.getTodayDashboard(jwtToken)
                if (response.success) {
                    _uiState.value = NutritionState.Dashboard(response.summary)
                } else {
                    _uiState.value = NutritionState.Error("Failed to load dashboard")
                }
            } catch (e: Exception) {
                _uiState.value = NutritionState.Error(e.message ?: "Network error")
            }
        }
    }

    fun analyzeImage(context: Context, uri: Uri, portionSize: Int) {
        viewModelScope.launch {
            _uiState.value = NutritionState.Loading
            try {
                val file = getFileFromUri(context, uri)
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                val portionBody = portionSize.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                val response = RetrofitClient.nutritionApi.analyzeFoodImage(jwtToken, imagePart, portionBody)

                if (response.success) {
                    _uiState.value = NutritionState.AiReview(response.data)
                } else {
                    _uiState.value = NutritionState.Error("AI Analysis failed")
                }
            } catch (e: Exception) {
                _uiState.value = NutritionState.Error(e.message ?: "Analysis error")
            }
        }
    }

    fun consumeAndTrackMeal(data: AiFoodData) {
        viewModelScope.launch {
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
                    fetchDashboard() // Sync complete, reload the dashboard
                }
            } catch (e: Exception) {
                _uiState.value = NutritionState.Error(e.message ?: "Failed to save meal")
            }
        }
    }

    fun cancelAiReview() {
        fetchDashboard()
    }

    private fun getFileFromUri(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(tempFile)
        inputStream?.copyTo(outputStream)
        return tempFile
    }
}