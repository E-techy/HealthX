package com.example.healthx.permissions_manager

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: ComponentActivity) {

    // 1. Define the mandatory runtime permissions
    private val requiredRuntimePermissions = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    ).apply {
        // Post notifications and Media Audio required for Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_MEDIA_AUDIO) // Needed to play local MP3 alarm tones
        } else {
            // Older storage permission for Android 12 and below
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    // 2. Register the multi-permission launcher for runtime prompts
    private val runtimePermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.all { it.value }
        if (allGranted) {
            // Check special permissions next after runtime ones are cleared
            checkAndRequestSpecialPermissions()
        } else {
            handlePermissionFailure("Mandatory runtime permissions denied.")
        }
    }

    /**
     * Core entry point. Call this in MainActivity's onCreate()
     */
    fun checkAndRequestAllPermissions() {
        if (hasAllRuntimePermissions()) {
            checkAndRequestSpecialPermissions()
        } else {
            runtimePermissionLauncher.launch(requiredRuntimePermissions)
        }
    }

    /**
     * Checks if standard permissions (Camera, Mic, Notifications, Storage) are already granted
     */
    private fun hasAllRuntimePermissions(): Boolean {
        return requiredRuntimePermissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Handles deep system configuration requirements like exact alarm scheduling
     * and full-screen intents (waking up the device).
     */
    private fun checkAndRequestSpecialPermissions() {
        // 1. Exact Alarm Permission (Android 12 / API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Direct redirect to the specific system setting screen for this app
                Toast.makeText(activity, "Please enable Exact Alarms for HealthX", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${activity.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                activity.startActivity(intent)
                return // Halt flow until they return
            }
        }

        // 2. Full-Screen Intent Permission (Android 14 / API 34+)
        // Crucial for popping the alarm screen over the lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.canUseFullScreenIntent()) {
                Toast.makeText(activity, "Please allow Full Screen Intents so alarms can wake your screen.", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:${activity.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                activity.startActivity(intent)
                return // Halt flow until they return
            }
        }

        // If execution reaches here, all criteria are fully met
        onAllPermissionsSecured()
    }

    /**
     * Logic executed when the security checks pass completely
     */
    private fun onAllPermissionsSecured() {
        Toast.makeText(activity, "All permissions validated successfully", Toast.LENGTH_SHORT).show()
    }

    /**
     * Strictly terminates app execution if conditions are not met
     */
    private fun handlePermissionFailure(message: String) {
        // Just show a warning, don't kill the app so we can test the UI!
        Toast.makeText(activity, "$message Some features may not work.", Toast.LENGTH_LONG).show()
    }

    /**
     * Call this in your activity's onResume() to catch when the user returns from settings
     */
    fun checkOnResume() {
        if (hasAllRuntimePermissions()) {

            // Re-verify Exact Alarms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) return
            }

            // Re-verify Full Screen Intents
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (!notificationManager.canUseFullScreenIntent()) return
            }

            // If they made it through both checks, permissions are secured
            onAllPermissionsSecured()
        }
    }
}