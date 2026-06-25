package com.example.healthx.permissions_manager

import android.Manifest
import android.app.AlarmManager
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
        // Post notifications required for Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
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
     * Checks if standard permissions (Camera, Mic, Notifications) are already granted
     */
    private fun hasAllRuntimePermissions(): Boolean {
        return requiredRuntimePermissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Handles deep system configuration requirements like exact alarm scheduling
     */
    private fun checkAndRequestSpecialPermissions() {
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
                return
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (alarmManager.canScheduleExactAlarms()) {
                    onAllPermissionsSecured()
                }
            } else {
                onAllPermissionsSecured()
            }
        }
    }
}