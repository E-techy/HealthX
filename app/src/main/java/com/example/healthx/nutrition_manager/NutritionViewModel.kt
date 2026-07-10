package com.example.healthx.nutrition_manager

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthx.data.local.SessionManager
import com.example.healthx.data.model.AnalyzeNutritionResponse
import com.example.healthx.data.network.RetrofitClient
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

// Defines exactly where the user is in the flow
sealed class NutritionScreenState {
    object Home : NutritionScreenState()
    object Scanner : NutritionScreenState()
    object AmountInput : NutritionScreenState()
    object Loading : NutritionScreenState()
    data class Error(val message: String) : NutritionScreenState()
    data class Success(val data: AnalyzeNutritionResponse) : NutritionScreenState()
}

// CHANGED: Now extends AndroidViewModel to access Application context for SessionManager
class NutritionViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "NutritionViewModel"
    private val sessionManager = SessionManager(application) // ADDED: SessionManager instance

    var currentScreen = mutableStateOf<NutritionScreenState>(NutritionScreenState.Home)
        private set

    val selectedImages = mutableStateListOf<Uri>()
    var mealAmountInput = mutableStateOf("")

    fun navigateTo(screen: NutritionScreenState) {
        Log.d(TAG, "Navigating to state: ${screen::class.java.simpleName}")
        currentScreen.value = screen
    }

    fun addImage(uri: Uri) {
        if (selectedImages.size < 10 && !selectedImages.contains(uri)) {
            selectedImages.add(uri)
            Log.d(TAG, "Added image: $uri. Total images: ${selectedImages.size}")
        } else {
            Log.w(TAG, "Failed to add image. Already exists or limit (10) reached. Current size: ${selectedImages.size}")
        }
    }

    fun removeImage(uri: Uri) {
        if (selectedImages.remove(uri)) {
            Log.d(TAG, "Removed image: $uri. Remaining images: ${selectedImages.size}")
        }
    }

    // Helper to get the real token
    private suspend fun getAuthToken(): String? {
        val account = sessionManager.activeAccountFlow.firstOrNull() ?: return null
        return "Bearer ${account.token}"
    }

    fun analyzeMeal(imageFiles: List<File>) {
        Log.i(TAG, "Starting meal analysis with ${imageFiles.size} images.")

        if (imageFiles.isEmpty()) {
            Log.e(TAG, "Abort analysis: No images provided.")
            navigateTo(NutritionScreenState.Error("Please provide at least one image."))
            return
        }

        navigateTo(NutritionScreenState.Loading)

        viewModelScope.launch {
            try {
                // FIXED: Fetching the REAL token from SessionManager
                val token = getAuthToken()
                if (token == null) {
                    Log.e(TAG, "❌ Fetch failed: Active Session Token is missing or NULL")
                    navigateTo(NutritionScreenState.Error("Session token missing. Please re-authenticate."))
                    return@launch
                }

                Log.d(TAG, "Preparing image parts...")
                val imageParts = imageFiles.map { file ->
                    // Explicitly declare it as a JPEG since FileUtil compresses it to JPEG
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("images", file.name, requestFile)
                }

                Log.d(TAG, "Preparing text fields. Input Amount: '${mealAmountInput.value}'")
                val userProfilePart = NutritionHelper.createPartFromString(NutritionHelper.getFakeUserProfile())
                val amountPart = NutritionHelper.createPartFromString(mealAmountInput.value)

                Log.i(TAG, "Executing network call using real Token prefix: ${token.take(15)}...")

                val response = RetrofitClient.nutritionApi.analyzeFoodImages(
                    token = token, // FIXED: Passing the real Bearer token
                    images = imageParts,
                    userInputAmount = amountPart,
                    userProfile = userProfilePart
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.i(TAG, "API Call Successful! MealId: ${body.mealId}, Items detected: ${body.data?.foodItems?.size ?: 0}")
                        navigateTo(NutritionScreenState.Success(body))
                    } else {
                        Log.e(TAG, "API Call Successful but body is null!")
                        navigateTo(NutritionScreenState.Error("Received empty response from server."))
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown server error"
                    val code = response.code()
                    Log.e(TAG, "API Call Failed. HTTP Code: $code, ErrorBody: $errorBody")
                    navigateTo(NutritionScreenState.Error("Server error ($code): $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during meal analysis: ${e.message}", e)
                navigateTo(NutritionScreenState.Error(e.message ?: "Network failure. Please try again."))
            }
        }
    }
}