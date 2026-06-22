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

    // We observe the active account to pass it directly to the HomeScreen
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val activeAccount by sessionManager.activeAccountFlow.collectAsState(initial = null)

    // UI Routing States to manually override the launcher
    var showAuthScreen by remember { mutableStateOf(false) }
    var showHomeScreen by remember { mutableStateOf(false) }

    // ROUTE 1: If Home is triggered and we have an account, show Home!
    if (showHomeScreen && activeAccount != null) {
        HomeScreen(
            account = activeAccount!!,
            onLogoutRequested = {
                // For testing purposes, reset states to go back to the beginning
                showHomeScreen = false
                showAuthScreen = false
            }
        )
        return // Stop executing the rest of the UI
    }

    // ROUTE 2: If Auth is triggered (via no accounts OR clicking "Add Account"), show Auth!
    if (showAuthScreen) {
        AuthNavGraph(
            onAuthSuccess = { message ->
                Log.d("AuthFlow", "Login Success: $message")
                // Turn off Auth screen and turn on Home screen
                showAuthScreen = false
                showHomeScreen = true
            }
        )
        return // Stop executing the rest of the UI
    }

    // ROUTE 3: Initial Launch Logic
    when (val state = launchState) {
        is LaunchState.Loading -> {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        is LaunchState.NoAccounts -> {
            // Automatically trigger the Auth flow if zero accounts exist
            LaunchedEffect(Unit) {
                showAuthScreen = true
            }
        }

        is LaunchState.SingleAccount -> {
            // Automatically trigger Home if exactly one account exists
            LaunchedEffect(Unit) {
                showHomeScreen = true
            }
        }

        is LaunchState.MultipleAccounts -> {
            AccountSelectionScreen(
                accounts = state.accounts,
                onAccountSelected = { account ->
                    launchViewModel.selectAccount(account) { showHomeScreen = true }
                },
                onAddNewAccount = { showAuthScreen = true },
                onRemoveAccount = { accountId ->
                    launchViewModel.removeAccount(accountId)
                }
            )
        }
    }
}