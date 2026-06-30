package com.example.healthx.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthx.data.local.SessionManager
import com.example.healthx.data.network.CreateOrderRequest
import com.example.healthx.data.network.OrderData
import com.example.healthx.data.network.SubscriptionApi
import com.example.healthx.data.network.SubscriptionPlan
import com.example.healthx.data.network.VerifyPaymentRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed class SubscriptionState {
    object Idle : SubscriptionState()
    object Loading : SubscriptionState()
    data class PlansLoaded(val plans: List<SubscriptionPlan>) : SubscriptionState()
    data class SinglePlanLoaded(val plan: SubscriptionPlan) : SubscriptionState()
    data class OrderCreated(val orderData: OrderData) : SubscriptionState()
    object PaymentVerified : SubscriptionState()
    data class Error(val message: String) : SubscriptionState()
    object NotLoggedIn : SubscriptionState() // <-- NEW STATE ADDED
}

class SubscriptionViewModel(
    private val api: SubscriptionApi,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<SubscriptionState>(SubscriptionState.Idle)
    val uiState: StateFlow<SubscriptionState> = _uiState.asStateFlow()

    fun fetchAllPlans() {
        viewModelScope.launch {
            _uiState.value = SubscriptionState.Loading
            try {
                val response = api.getAllPlans()
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = SubscriptionState.PlansLoaded(response.body()?.data ?: emptyList())
                } else {
                    _uiState.value = SubscriptionState.Error(response.body()?.message ?: "Failed to load plans")
                }
            } catch (e: Exception) {
                _uiState.value = SubscriptionState.Error(e.message ?: "Network error")
            }
        }
    }

    fun fetchPlanDetails(subscriptionId: String) {
        viewModelScope.launch {
            _uiState.value = SubscriptionState.Loading
            try {
                val response = api.getPlanById(subscriptionId)
                if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                    _uiState.value = SubscriptionState.SinglePlanLoaded(response.body()!!.data!!)
                } else {
                    _uiState.value = SubscriptionState.Error("Plan not found")
                }
            } catch (e: Exception) {
                _uiState.value = SubscriptionState.Error(e.message ?: "Network error")
            }
        }
    }

    fun initiateCheckout(subscriptionId: String) {
        viewModelScope.launch {
            _uiState.value = SubscriptionState.Loading

            val token = getBearerToken()
            // Check if the user is logged in before hitting the protected route
            if (token == null) {
                _uiState.value = SubscriptionState.NotLoggedIn
                return@launch
            }

            try {
                val response = api.createOrder(token, CreateOrderRequest(subscriptionId))
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = SubscriptionState.OrderCreated(response.body()!!.orderData!!)
                } else {
                    _uiState.value = SubscriptionState.Error(response.body()?.message ?: "Failed to create order")
                }
            } catch (e: Exception) {
                _uiState.value = SubscriptionState.Error(e.message ?: "Network error")
            }
        }
    }

    fun verifyPayment(orderId: String, paymentId: String, signature: String) {
        viewModelScope.launch {
            _uiState.value = SubscriptionState.Loading

            val token = getBearerToken()
            if (token == null) {
                _uiState.value = SubscriptionState.NotLoggedIn
                return@launch
            }

            try {
                val request = VerifyPaymentRequest(orderId, paymentId, signature)
                val response = api.verifyPayment(token, request)
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = SubscriptionState.PaymentVerified
                } else {
                    _uiState.value = SubscriptionState.Error(response.body()?.message ?: "Verification failed")
                }
            } catch (e: Exception) {
                _uiState.value = SubscriptionState.Error(e.message ?: "Network error")
            }
        }
    }

    fun resetState() {
        _uiState.value = SubscriptionState.Idle
    }

    private suspend fun getBearerToken(): String? {
        val account = sessionManager.activeAccountFlow.firstOrNull()
        // If it's a guest account, treat as logged out for payments
        if (account?.isGuest == true) return null
        return account?.token?.let { "Bearer $it" }
    }
}

class SubscriptionViewModelFactory(
    private val api: SubscriptionApi,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscriptionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubscriptionViewModel(api, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}