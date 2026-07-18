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

    companion object {
        // This singleton flag ensures we only aggressively harass the user ONCE per app lifecycle.
        // If they deny or are thrown to settings, we won't loop them again when they return to the Home Screen.
        private var hasRunPermissionFlowThisSession = false
    }

    private val requiredRuntimePermissions = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    private val runtimePermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.all { it.value }
        if (allGranted) {
            checkAndRequestSpecialPermissions()
        } else {
            handlePermissionFailure("Critical permissions denied.")
        }
    }

    fun checkAndRequestAllPermissions() {
        // STOP THE INFINITE LOOP: Only execute this aggressive flow once per app launch.
        if (hasRunPermissionFlowThisSession) return

        if (hasAllRuntimePermissions()) {
            checkAndRequestSpecialPermissions()
        } else {
            // Mark that we are executing the flow so we don't do it again if they minimize/return
            hasRunPermissionFlowThisSession = true
            runtimePermissionLauncher.launch(requiredRuntimePermissions)
        }
    }

    private fun hasAllRuntimePermissions(): Boolean {
        return requiredRuntimePermissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkAndRequestSpecialPermissions() {
        // 1. Exact Alarm Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                hasRunPermissionFlowThisSession = true // Mark as run before throwing to settings
                Toast.makeText(activity, "CRITICAL: Please enable Exact Alarms for HealthX", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${activity.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                activity.startActivity(intent)
                return
            }
        }

        // 2. Full-Screen Intent Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.canUseFullScreenIntent()) {
                hasRunPermissionFlowThisSession = true // Mark as run before throwing to settings
                Toast.makeText(activity, "CRITICAL: Please allow Full Screen Intents so alarms can wake your screen.", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:${activity.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                activity.startActivity(intent)
                return
            }
        }

        // If we reach here, all permissions are granted
        if (!hasRunPermissionFlowThisSession) {
            hasRunPermissionFlowThisSession = true
            onAllPermissionsSecured()
        }
    }

    private fun onAllPermissionsSecured() {
        Toast.makeText(activity, "System Ready: All Permissions Secured", Toast.LENGTH_SHORT).show()
    }

    /**
     * STRICT ENFORCEMENT: If they deny a permission, force them to the settings screen.
     */
    private fun handlePermissionFailure(message: String) {
        hasRunPermissionFlowThisSession = true // Ensure we don't loop
        Toast.makeText(activity, "$message The app cannot function. Please enable them in settings.", Toast.LENGTH_LONG).show()

        // Throw user directly into the Android System Settings page for this app
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${activity.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        activity.startActivity(intent)
    }

    fun checkOnResume() {
        // External logic stays identical. Passive checks only.
        if (hasAllRuntimePermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (!notificationManager.canUseFullScreenIntent()) return
            }

            // Only trigger if we aren't currently waiting on a setting flip
            // onAllPermissionsSecured()
        }
    }
}