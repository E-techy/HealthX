package com.example.healthx

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.example.healthx.permissions_manager.PermissionManager
import com.example.healthx.ui.screens.reminders.RemindersNavGraph
import com.example.healthx.ui.theme.HealthXTheme
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager
    private val TAG = "FCM_TOKEN_TEST"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize and fire the permission validation script
        permissionManager = PermissionManager(this)
        permissionManager.checkAndRequestAllPermissions()

        // Fetch and Log the FCM Token for Backend Testing
        fetchFCMToken()

        setContent {
            HealthXTheme {
                RemindersNavGraph()
            }
        }
    }

    private fun fetchFCMToken() {
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

            // Optional: Toast so you see it visually on the device
            Toast.makeText(baseContext, "FCM Token Logged!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        permissionManager.checkOnResume()
    }
}