package com.example.healthx.nutrition_manager

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthx.data.network.AnalyzeNutritionResponse
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

    var currentScreen = mutableStateOf<NutritionScreenState>(NutritionScreenState.Home)
        private set

    // Holds images from both Camera and Gallery
    val selectedImages = mutableStateListOf<Uri>()
    var mealAmountInput = mutableStateOf("")

    fun navigateTo(screen: NutritionScreenState) {
        currentScreen.value = screen
    }

    fun addImage(uri: Uri) {
        if (selectedImages.size < 10 && !selectedImages.contains(uri)) {
            selectedImages.add(uri)
        }
    }

    fun removeImage(uri: Uri) {
        selectedImages.remove(uri)
    }

    fun analyzeMeal(imageFiles: List<File>) {
        navigateTo(NutritionScreenState.Loading)

        viewModelScope.launch {
            try {
                // 1. Prepare Images
                val imageParts = imageFiles.map { file ->
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("images", file.name, requestFile)
                }

                // 2. Prepare Text Fields
                val userProfilePart = NutritionHelper.createPartFromString(NutritionHelper.getFakeUserProfile())
                val amountPart = NutritionHelper.createPartFromString(mealAmountInput.value)

                // 3. Make the Network Call (Using the singleton from previous step)
                val response = RetrofitClient.nutritionApi.analyzeFoodImages(
                    token = "Bearer FAKE_JWT_TOKEN", // Replace with real token manager later
                    images = imageParts,
                    userInputAmount = amountPart,
                    userProfile = userProfilePart
                    // apiKey = NutritionHelper.getApiKeyPart(), // Commented out per instructions
                    // modelName = NutritionHelper.getModelNamePart()
                )

                if (response.isSuccessful && response.body() != null) {
                    navigateTo(NutritionScreenState.Success(response.body()!!))
                } else {
                    // Extract exact error message from backend
                    val errorBody = response.errorBody()?.string() ?: "Unknown server error"
                    navigateTo(NutritionScreenState.Error(errorBody))
                }
            } catch (e: Exception) {
                navigateTo(NutritionScreenState.Error(e.message ?: "Network failure. Please try again."))
            }
        }
    }
}