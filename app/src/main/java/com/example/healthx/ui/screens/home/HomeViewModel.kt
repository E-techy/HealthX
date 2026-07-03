package com.example.healthx.ui.screens.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthx.data.local.SavedAccount
import com.example.healthx.data.local.SessionManager
import com.example.healthx.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val api = RetrofitClient.homeApi // Assumes you added homeApi to RetrofitClient

    private val _subscriptionStatus = MutableStateFlow("Loading...")
    val subscriptionStatus: StateFlow<String> = _subscriptionStatus.asStateFlow()

    fun fetchSubscriptionStatus(account: SavedAccount) {
        if (account.isGuest) {
            _subscriptionStatus.value = "GUEST"
            return
        }

        viewModelScope.launch {
            try {
                val token = "Bearer ${account.token}"
                val response = api.getSubscriptionStatus(token)

                if (response.isSuccessful && response.body() != null) {
                    _subscriptionStatus.value = response.body()!!.status
                } else {
                    _subscriptionStatus.value = "FREE" // Default fallback
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to fetch status: ${e.message}")
                _subscriptionStatus.value = "FREE" // Offline fallback
            }
        }
    }

    fun switchAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            sessionManager.switchActiveAccount("") // Clears active account
            onSuccess()
        }
    }

    fun logout(accountId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            sessionManager.removeAccount(accountId) // Deletes account locally
            onSuccess()
        }
    }
}