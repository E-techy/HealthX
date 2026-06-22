package com.example.healthx

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthx.data.local.SessionManager
import com.example.healthx.ui.screens.auth.AuthNavGraph
import com.example.healthx.ui.screens.home.HomeScreen
import com.example.healthx.ui.screens.launch.AccountSelectionScreen
import com.example.healthx.ui.screens.launch.AccountSelectionViewModel
import com.example.healthx.ui.screens.launch.LaunchState
import com.example.healthx.ui.theme.HealthXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            HealthXTheme {
                RootApp()
            }
        }
    }
}

@Composable
fun RootApp() {
    val launchViewModel: AccountSelectionViewModel = viewModel()
    val launchState by launchViewModel.launchState.collectAsState()

    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    // Collect the full list of accounts to know if we have multiples
    val accountsList by sessionManager.savedAccountsFlow.collectAsState(initial = emptyList())
    val activeAccount by sessionManager.activeAccountFlow.collectAsState(initial = null)

    var showAuthScreen by remember { mutableStateOf(false) }
    var showHomeScreen by remember { mutableStateOf(false) }

    // ROUTE 1: Home Screen
    if (showHomeScreen && activeAccount != null) {
        HomeScreen(
            account = activeAccount!!,
            hasMultipleAccounts = accountsList.size > 1,
            onLogoutRequested = {
                // Temporary logout logic for testing
                showHomeScreen = false
                showAuthScreen = true
            },
            onSwitchAccountRequested = {
                // Turn off both manual overrides so it falls back to LaunchState (which will show the picker)
                showHomeScreen = false
                showAuthScreen = false
            }
        )
        return
    }

    // ROUTE 2: Auth Screen
    if (showAuthScreen) {
        AuthNavGraph(
            onAuthSuccess = { message ->
                Log.d("AuthFlow", "Login Success: $message")
                showAuthScreen = false
                showHomeScreen = true
            },
            onBackToAccounts = {
                // If they click "Back to Accounts" from the login screen
                showAuthScreen = false
            }
        )
        return
    }

    // ROUTE 3: Base Launch Logic
    when (val state = launchState) {
        is LaunchState.Loading -> {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        is LaunchState.NoAccounts -> {
            LaunchedEffect(Unit) { showAuthScreen = true }
        }

        is LaunchState.SingleAccount -> {
            // Check dynamically: if they deleted an account and dropped to 1, or just booted up with 1
            if (accountsList.size <= 1) {
                LaunchedEffect(Unit) { showHomeScreen = true }
            } else {
                // If state hasn't caught up to reality, force the picker
                AccountSelectionScreen(
                    accounts = accountsList,
                    onAccountSelected = { account ->
                        launchViewModel.selectAccount(account) { showHomeScreen = true }
                    },
                    onAddNewAccount = { showAuthScreen = true },
                    onRemoveAccount = { accountId, onSuccess ->
                        launchViewModel.removeAccount(accountId, onSuccess)
                    }
                )
            }
        }

        is LaunchState.MultipleAccounts -> {
            AccountSelectionScreen(
                accounts = state.accounts,
                onAccountSelected = { account ->
                    launchViewModel.selectAccount(account) { showHomeScreen = true }
                },
                onAddNewAccount = { showAuthScreen = true },
                onRemoveAccount = { accountId, onSuccess ->
                    launchViewModel.removeAccount(accountId, onSuccess)
                }
            )
        }
    }
}