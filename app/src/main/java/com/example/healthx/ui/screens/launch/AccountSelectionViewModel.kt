package com.example.healthx.ui.screens.launch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthx.data.local.SavedAccount
import com.example.healthx.data.local.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class LaunchState {
    object Loading : LaunchState()
    object NoAccounts : LaunchState()
    data class SingleAccount(val account: SavedAccount) : LaunchState()
    data class MultipleAccounts(val accounts: List<SavedAccount>) : LaunchState()
}

class AccountSelectionViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)

    private val _launchState = MutableStateFlow<LaunchState>(LaunchState.Loading)
    val launchState: StateFlow<LaunchState> = _launchState.asStateFlow()

    init {
        determineLaunchPath()
    }

    private fun determineLaunchPath() {
        viewModelScope.launch {
            // Read the permanent storage once on startup
            val accounts = sessionManager.savedAccountsFlow.first()

            _launchState.value = when (accounts.size) {
                0 -> LaunchState.NoAccounts
                1 -> {
                    // Automatically set the single account as active
                    sessionManager.switchActiveAccount(accounts.first().accountId)
                    LaunchState.SingleAccount(accounts.first())
                }
                else -> LaunchState.MultipleAccounts(accounts)
            }
        }
    }

    fun selectAccount(account: SavedAccount, onSelected: () -> Unit) {
        viewModelScope.launch {
            sessionManager.switchActiveAccount(account.accountId)
            onSelected()
        }
    }

    fun removeAccount(accountId: String) {
        viewModelScope.launch {
            // Call your new utility function
            com.example.healthx.utils.AccountUtils.completelyRemoveAccount(getApplication(), accountId)

            // The UI will automatically refresh because it is observing sessionManager.savedAccountsFlow
        }
    }
}