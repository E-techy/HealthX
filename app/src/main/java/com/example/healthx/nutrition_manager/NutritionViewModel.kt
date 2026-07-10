package com.example.healthx.nutrition_manager

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Updated to the correct model package
import com.example.healthx.data.model.AnalyzeNutritionResponse
import com.example.healthx.data.network.RetrofitClient
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

class NutritionViewModel : ViewModel() {

    private val TAG = "NutritionViewModel"

    var currentScreen = mutableStateOf<NutritionScreenState>(NutritionScreenState.Home)
        private set

    // Holds images from both Camera and Gallery
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
                Log.d(TAG, "Preparing image parts...")
                // 1. Prepare Images
                val imageParts = imageFiles.map { file ->
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("images", file.name, requestFile)
                }

                Log.d(TAG, "Preparing text fields. Input Amount: '${mealAmountInput.value}'")
                // 2. Prepare Text Fields
                val userProfilePart = NutritionHelper.createPartFromString(NutritionHelper.getFakeUserProfile())
                val amountPart = NutritionHelper.createPartFromString(mealAmountInput.value)

                Log.i(TAG, "Executing network call to RetrofitClient.nutritionApi.analyzeFoodImages...")
                // 3. Make the Network Call
                val response = RetrofitClient.nutritionApi.analyzeFoodImages(
                    token = "Bearer FAKE_JWT_TOKEN", // Replace with real token manager later
                    images = imageParts,
                    userInputAmount = amountPart,
                    userProfile = userProfilePart
                    // apiKey = NutritionHelper.getApiKeyPart(),
                    // modelName = NutritionHelper.getModelNamePart()
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