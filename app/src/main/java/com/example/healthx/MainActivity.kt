package com.example.healthx

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthx.data.local.SessionManager
import com.example.healthx.permissions_manager.PermissionManager
import com.example.healthx.ui.screens.auth.AuthNavGraph
import com.example.healthx.ui.screens.launch.AccountSelectionScreen
import com.example.healthx.ui.screens.launch.AccountSelectionViewModel
import com.example.healthx.ui.screens.reminders.RemindersNavGraph
import com.example.healthx.ui.theme.HealthXTheme
import com.example.healthx.utils.LocalActiveAccount
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager
    private val TAG = "FCM_TOKEN_TEST"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 1. Explicitly initialize Firebase
        FirebaseApp.initializeApp(this)

        // 2. Initialize and fire the permission validation script
        permissionManager = PermissionManager(this)
        permissionManager.checkAndRequestAllPermissions()

        // 3. Fetch and Log the FCM Token for Backend Testing
        fetchFCMToken()

        setContent {
            HealthXTheme {
                RootScreen()
            }
        }
    }

    /**
     * RootScreen acts as the Grand Central Station for your app.
     * It observes the DataStore flows and automatically routes the user based on their login state.
     */
    @Composable
    fun RootScreen() {
        val context = LocalContext.current
        val sessionManager = remember { SessionManager(context) }

        // Reactively observe local storage
        val activeAccount by sessionManager.activeAccountFlow.collectAsState(initial = null)
        val savedAccounts by sessionManager.savedAccountsFlow.collectAsState(initial = null) // null = loading

        // State toggle to force the Auth UI when a user clicks "Add New Account"
        var forceShowAuth by remember { mutableStateOf(false) }

        // PATH 1: Loading State (Waiting for DataStore to read from disk)
        if (savedAccounts == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        // PATH 2: Main App (User is authenticated and has an active session)
        if (activeAccount != null && !forceShowAuth) {
            // Provide the real account to the rest of the app (No more dummy account!)
            CompositionLocalProvider(LocalActiveAccount provides activeAccount!!) {
                RemindersNavGraph()
            }
        }

        // PATH 3: Account Selector (Logged out, but accounts exist on the device)
        else if (savedAccounts!!.isNotEmpty() && !forceShowAuth) {
            val accountViewModel: AccountSelectionViewModel = viewModel()

            AccountSelectionScreen(
                accounts = savedAccounts!!,
                onAccountSelected = { account ->
                    accountViewModel.selectAccount(account) {
                        // Action completed: 'activeAccount' flow will automatically emit the new account,
                        // instantly recomposing the UI into PATH 2 (RemindersNavGraph).
                    }
                },
                onAddNewAccount = {
                    forceShowAuth = true // Switch to PATH 4
                },
                onRemoveAccount = { id, onSuccess ->
                    accountViewModel.removeAccount(id, onSuccess)
                }
            )
        }

        // PATH 4: Authentication Flow (No accounts exist OR user clicked "Add New Account")
        else {
            AuthNavGraph(
                onAuthSuccess = { accountId ->
                    forceShowAuth = false
                    // Your AuthViewModel saves the account to SessionManager.
                    // The 'activeAccount' flow will automatically pick this up and route to PATH 2.
                },
                onBackToAccounts = {
                    forceShowAuth = false // Hides Auth Graph and returns to Account Selector
                }
            )
        }
    }

    private fun fetchFCMToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d(TAG, "==============================================")
                Log.d(TAG, "YOUR FCM TOKEN IS: $token")
                Log.d(TAG, "==============================================")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase not initialized or missing dependency: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        permissionManager.checkOnResume()
    }
}