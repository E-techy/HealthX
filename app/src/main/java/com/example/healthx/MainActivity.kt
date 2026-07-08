package com.example.healthx

import android.content.Intent
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.healthx.data.local.SessionManager
import com.example.healthx.notification_manager.FcmTokenSyncManager
import com.example.healthx.permissions_manager.PermissionManager
import com.example.healthx.ui.screens.auth.AuthNavGraph
import com.example.healthx.ui.screens.chat.ChatScreen
import com.example.healthx.ui.screens.home.HomeScreen
import com.example.healthx.ui.screens.launch.AccountSelectionScreen
import com.example.healthx.ui.screens.launch.AccountSelectionViewModel
import com.example.healthx.ui.screens.reminders.RemindersNavGraph
import com.example.healthx.ui.screens.scanner.QRScannerScreen
import com.example.healthx.ui.subscription.SubscriptionActivity
// IMPORT THE NUTRITION ACTIVITY
import com.example.healthx.ui.screens.nutrition.NutritionActivity
import com.example.healthx.ui.theme.HealthXTheme
import com.example.healthx.utils.LocalActiveAccount
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager
    private val TAG = "FCM_TOKEN_TEST"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        FirebaseApp.initializeApp(this)

        permissionManager = PermissionManager(this)
        permissionManager.checkAndRequestAllPermissions()

        setContent {
            HealthXTheme {
                RootScreen()
            }
        }
    }

    @Composable
    fun RootScreen() {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val sessionManager = remember { SessionManager(context) }
        val fcmTokenSyncManager = remember { FcmTokenSyncManager(context) }

        val activeAccount by sessionManager.activeAccountFlow.collectAsState(initial = null)
        val savedAccounts by sessionManager.savedAccountsFlow.collectAsState(initial = null)
        var forceShowAuth by remember { mutableStateOf(false) }

        // Trigger the Smart FCM Sync in the background every time the app opens
        LaunchedEffect(Unit) {
            fcmTokenSyncManager.syncAllAccounts(FcmTokenSyncManager.SyncMode.ONLINE)
        }

        if (savedAccounts == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        // PATH 2: Main App
        if (activeAccount != null && !forceShowAuth) {
            CompositionLocalProvider(LocalActiveAccount provides activeAccount!!) {
                val mainNavController = rememberNavController()

                NavHost(navController = mainNavController, startDestination = "home") {

                    composable("home") {
                        HomeScreen(
                            account = activeAccount!!,
                            hasMultipleAccounts = savedAccounts!!.size > 1,
                            onNavigateToSettings = { /* ... */ },
                            onNavigateToApiKeys = { /* ... */ },
                            onNavigateToAiChat = { mainNavController.navigate("ai_chat") },
                            onNavigateToReminders = { mainNavController.navigate("reminders") },

                            onNavigateToAlarmManager = {
                                context.startActivity(Intent(context, com.example.healthx.alarm_manager.AlarmManagerActivity::class.java))
                            },

                            onNavigateToScanner = { mainNavController.navigate("scanner") },
                            onNavigateToSubscriptions = {
                                context.startActivity(Intent(context, SubscriptionActivity::class.java))
                            },

                            // ADDED: Launching the new Nutrition Activity
                            onNavigateToNutrition = {
                                context.startActivity(Intent(context, NutritionActivity::class.java))
                            },

                            onSwitchAccountRequested = { /* ... */ },
                            onLogoutRequested = { /* ... */ }
                        )
                    }

                    composable("reminders") {
                        RemindersNavGraph()
                    }

                    composable("scanner") {
                        QRScannerScreen(
                            onCloseClicked = { mainNavController.popBackStack() }
                        )
                    }

                    composable("ai_chat") {
                        ChatScreen(
                            onNavigateToSettings = { /* TODO: Route to settings if API key is missing */ }
                        )
                    }
                }
            }
        }
        else if (savedAccounts!!.isNotEmpty() && !forceShowAuth) {
            val accountViewModel: AccountSelectionViewModel = viewModel()

            AccountSelectionScreen(
                accounts = savedAccounts!!,
                onAccountSelected = { account ->
                    accountViewModel.selectAccount(account) { }
                },
                onAddNewAccount = {
                    forceShowAuth = true
                },
                onRemoveAccount = { id, onSuccess ->
                    accountViewModel.removeAccount(id, onSuccess)
                }
            )
        }
        else {
            AuthNavGraph(
                onAuthSuccess = { accountId ->
                    forceShowAuth = false
                },
                onBackToAccounts = {
                    forceShowAuth = false
                }
            )
        }
    }


    override fun onResume() {
        super.onResume()
        permissionManager.checkOnResume()
    }
}