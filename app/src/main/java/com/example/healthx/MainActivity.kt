package com.example.healthx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.example.healthx.permissions_manager.PermissionManager
import com.example.healthx.ui.screens.reminders.RemindersNavGraph
import com.example.healthx.ui.theme.HealthXTheme

class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize and fire the permission validation script
        permissionManager = PermissionManager(this)
        permissionManager.checkAndRequestAllPermissions()

        setContent {
            HealthXTheme {
                // If the app hasn't been terminated by the script, safely render the application graph
                RemindersNavGraph()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-verify flags in case the user returned from manual system settings toggle
        permissionManager.checkOnResume()
    }
}