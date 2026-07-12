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
import com.example.healthx.data.model.CreateGoalRequest
import com.example.healthx.data.model.MealHistoryItem
import com.example.healthx.data.model.NutritionGoal
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
    object MealsHistory : NutritionScreenState()
    object Goals : NutritionScreenState() // ADDED: Goals Screen State
    object CreateGoal : NutritionScreenState() // ADD THIS
}

class NutritionViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "NutritionViewModel"
    private val sessionManager = SessionManager(application)

    var currentScreen = mutableStateOf<NutritionScreenState>(NutritionScreenState.Home)
        private set

    fun navigateTo(screen: NutritionScreenState) {
        Log.d(TAG, "Navigating to state: ${screen::class.java.simpleName}")
        currentScreen.value = screen
    }

    private suspend fun getAuthToken(): String? {
        val account = sessionManager.activeAccountFlow.firstOrNull() ?: return null
        return "Bearer ${account.token}"
    }

    // ==========================================
    // 1. AI SCANNER & MEAL ANALYSIS STATE
    // ==========================================
    val selectedImages = mutableStateListOf<Uri>()
    var mealAmountInput = mutableStateOf("")

    fun addImage(uri: Uri) {
        if (selectedImages.size < 10 && !selectedImages.contains(uri)) {
            selectedImages.add(uri)
        }
    }

    fun removeImage(uri: Uri) {
        selectedImages.remove(uri)
    }

    fun analyzeMeal(imageFiles: List<File>) {
        if (imageFiles.isEmpty()) {
            navigateTo(NutritionScreenState.Error("Please provide at least one image."))
            return
        }

        navigateTo(NutritionScreenState.Loading)

        viewModelScope.launch {
            try {
                val token = getAuthToken()
                if (token == null) {
                    navigateTo(NutritionScreenState.Error("Session token missing. Please re-authenticate."))
                    return@launch
                }

                val imageParts = imageFiles.map { file ->
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("images", file.name, requestFile)
                }

                val userProfilePart = NutritionHelper.createPartFromString(NutritionHelper.getFakeUserProfile())
                val amountPart = NutritionHelper.createPartFromString(mealAmountInput.value)

                val response = RetrofitClient.nutritionApi.analyzeFoodImages(
                    token = token,
                    images = imageParts,
                    userInputAmount = amountPart,
                    userProfile = userProfilePart
                )

                if (response.isSuccessful && response.body() != null) {
                    navigateTo(NutritionScreenState.Success(response.body()!!))
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown server error"
                    navigateTo(NutritionScreenState.Error("Server error: $errorBody"))
                }
            } catch (e: Exception) {
                navigateTo(NutritionScreenState.Error(e.message ?: "Network failure. Please try again."))
            }
        }
    }

    // ==========================================
    // 2. MEALS HISTORY STATE
    // ==========================================
    var mealsHistoryList = mutableStateOf<List<MealHistoryItem>>(emptyList())
    var isFetchingHistory = mutableStateOf(false)
    var searchMealId = mutableStateOf("")
    var selectedDateFilter = mutableStateOf("Today")
    var selectedStatusFilter = mutableStateOf("All")

    fun fetchMealsHistory() {
        isFetchingHistory.value = true
        viewModelScope.launch {
            try {
                val token = getAuthToken() ?: return@launch

                val showParam = when (selectedStatusFilter.value) {
                    "Discarded" -> "discarded"
                    "All" -> "all"
                    else -> null
                }
                val idQuery = searchMealId.value.takeIf { it.isNotBlank() }

                val response = RetrofitClient.nutritionApi.getMealsHistory(
                    token = token,
                    show = showParam,
                    mealId = idQuery
                )

                if (response.isSuccessful) {
                    mealsHistoryList.value = response.body()?.data ?: emptyList()
                } else {
                    Log.e(TAG, "Failed to fetch history: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching history", e)
            } finally {
                isFetchingHistory.value = false
            }
        }
    }

    // ==========================================
    // 3. NUTRITION GOALS STATE
    // ==========================================
    var goalsList = mutableStateOf<List<NutritionGoal>>(emptyList())
    var isFetchingGoals = mutableStateOf(false)
    var selectedGoalFilter = mutableStateOf("Active") // "Active", "Expired", "All"

    fun fetchGoals() {
        isFetchingGoals.value = true
        viewModelScope.launch {
            try {
                val token = getAuthToken() ?: return@launch

                val showParam = when (selectedGoalFilter.value) {
                    "Expired" -> "expired"
                    "All" -> "all"
                    else -> null // Active
                }

                val response = RetrofitClient.nutritionApi.getGoals(token, showParam)
                if (response.isSuccessful) {
                    goalsList.value = response.body()?.data ?: emptyList()
                } else {
                    Log.e(TAG, "Failed to fetch goals: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching goals", e)
            } finally {
                isFetchingGoals.value = false
            }
        }
    }

    fun createNewGoal(request: CreateGoalRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = getAuthToken() ?: return@launch
                val response = RetrofitClient.nutritionApi.createGoal(token, request)
                if (response.isSuccessful) {
                    fetchGoals() // Refresh the list
                    onSuccess()
                } else {
                    Log.e(TAG, "Failed to create goal: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating goal", e)
            }
        }
    }
}