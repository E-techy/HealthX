package com.example.healthx

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import com.example.healthx.data.local.SavedAccount
import com.example.healthx.permissions_manager.PermissionManager
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

        // 1. Explicitly initialize Firebase to prevent "not referenced" errors
        FirebaseApp.initializeApp(this)

        // 2. Initialize and fire the permission validation script
        permissionManager = PermissionManager(this)
        permissionManager.checkAndRequestAllPermissions()

        // 3. Fetch and Log the FCM Token for Backend Testing
        fetchFCMToken()

        setContent {
            HealthXTheme {
                // 4. Create the dummy account so the UI doesn't crash!
                val dummyAccount = SavedAccount(
                    accountId = "dev_test_id",
                    email = "developer@healthx.com",
                    name = "Developer",
                    token = "dummy_token",
                    profilePhotoUrl = null,
                    isGuest = false
                )

                // 5. Wrap the UI in the provider to feed it the dummy account
                CompositionLocalProvider(LocalActiveAccount provides dummyAccount) {
                    RemindersNavGraph()
                }
            }
        }
    }

    private fun fetchFCMToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                // Get new FCM registration token
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