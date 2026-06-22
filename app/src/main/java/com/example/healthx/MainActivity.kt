package com.example.healthx

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthx.ui.screens.auth.AuthNavGraph
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

    when (val state = launchState) {
        is LaunchState.Loading -> {
            // Brief loading screen while checking DataStore
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        is LaunchState.NoAccounts -> {
            // Zero accounts saved -> Send straight to Login/Signup flow
            AuthNavGraph(
                onAuthSuccess = { message ->
                    Log.d("AuthFlow", "Login Success. Sending to Home. Message: $message")
                    // Note: In the future, we replace this log with a NavController route to HomeScreen
                }
            )
        }

        is LaunchState.SingleAccount -> {
            // Exactly one account -> Auto-login!
            // PLACEHOLDER: Route to your future HomeScreen
            Log.i("HealthX_Session", "Auto-Logged in as: ${state.account.name}. JWT: ${state.account.token}")

            // For now, to keep the app visually running while you build HomeScreen,
            // we will just throw them back to the AuthGraph so the app doesn't crash on an empty screen.
            // Replace AuthNavGraph() with HomeScreen() when it is built.
            AuthNavGraph(onAuthSuccess = {})
        }

        is LaunchState.MultipleAccounts -> {
            // Multiple accounts -> Show the sleek Account Picker
            AccountSelectionScreen(
                accounts = state.accounts,
                onAccountSelected = { account ->
                    launchViewModel.selectAccount(account) {
                        Log.i("HealthX_Session", "Switched to account: ${account.name}")
                        // Route to HomeScreen placeholder
                    }
                },
                onAddNewAccount = {
                    // Temporarily wipe the state or use navigation to force the AuthNavGraph to appear
                    Log.d("HealthX_Session", "User requested to add a new account. Routing to Login.")
                }
            )
        }
    }
}