package com.example.healthx.notification_manager

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.healthx.BuildConfig
import com.example.healthx.data.local.SavedAccount
import com.example.healthx.data.local.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class FcmTokenSyncManager(private val context: Context) {

    private val TAG = "FcmTokenSync"
    private val sessionManager = SessionManager(context)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val sharedPreferences = context.getSharedPreferences("HealthX_FCM_Prefs", Context.MODE_PRIVATE)
    private val SAVED_TOKEN_KEY = "CLOUD_MIRRORED_FCM_TOKEN"
    private val DEVICE_ID_KEY = "HEALTHX_DEVICE_ID"

    // The endpoint your server will eventually use to register device tokens
    private val BACKEND_URL = "${BuildConfig.BASE_URL}device/sync-token" // Adjust path as needed

    enum class SyncMode {
        ONLINE,  // Forces a network request to the backend for all accounts
        OFFLINE  // Smart check: only networks if local token != real FCM token
    }

    /**
     * Gets a permanent, unique ID for this specific app installation.
     */
    fun getDeviceId(): String {
        var deviceId = sharedPreferences.getString(DEVICE_ID_KEY, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            sharedPreferences.edit().putString(DEVICE_ID_KEY, deviceId).apply()
        }
        return deviceId
    }

    /**
     * Gets the hardware manufacturer and model (e.g., "Samsung SM-G998B")
     */
    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.lowercase().startsWith(manufacturer.lowercase())) {
            model.replaceFirstChar { it.uppercase() }
        } else {
            "${manufacturer.replaceFirstChar { it.uppercase() }} $model"
        }
    }

    /**
     * Main entry point. Loops through all logged-in accounts and syncs the token.
     */
    suspend fun syncAllAccounts(syncMode: SyncMode = SyncMode.OFFLINE) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting Token Sync Process. Mode: $syncMode")

                val realToken = FirebaseMessaging.getInstance().token.await()
                val locallySavedToken = sharedPreferences.getString(SAVED_TOKEN_KEY, null)

                if (syncMode == SyncMode.OFFLINE && locallySavedToken == realToken) {
                    Log.d(TAG, "Token is up-to-date. No network requests needed.")
                    return@withContext
                }

                // Token has changed or force sync requested. Fetch all accounts.
                val accounts = sessionManager.savedAccountsFlow.first()

                if (accounts.isEmpty()) {
                    Log.d(TAG, "No logged-in accounts to sync with. Storing token locally for future logins.")
                    saveTokenLocally(realToken)
                    return@withContext
                }

                val deviceId = getDeviceId()
                val deviceName = getDeviceName()
                var allSyncsSuccessful = true

                // Loop through every account and send an authenticated request
                for (account in accounts) {
                    if (account.isGuest) continue // Don't sync push tokens for offline guest accounts

                    Log.d(TAG, "Syncing token for account: ${account.email}")
                    val success = uploadTokenToServer(account.token, deviceId, deviceName, realToken)

                    if (!success) {
                        Log.e(TAG, "Failed to sync token for account: ${account.email}")
                        allSyncsSuccessful = false
                    }
                }

                // If EVERY account successfully updated on the server, update the local mirrored token.
                // If even one failed, we skip this, meaning the app will retry the whole loop next time it opens.
                if (allSyncsSuccessful) {
                    saveTokenLocally(realToken)
                    Log.d(TAG, "✅ Token sync complete for all accounts.")
                } else {
                    Log.w(TAG, "⚠️ Some accounts failed to sync. Will retry on next launch.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Critical error during FCM token sync: ${e.message}")
            }
        }
    }

    /**
     * Syncs the token for a SINGLE account. (Use this immediately after a user successfully logs in).
     */
    suspend fun syncSingleAccount(account: SavedAccount) {
        withContext(Dispatchers.IO) {
            try {
                if (account.isGuest) return@withContext
                val realToken = FirebaseMessaging.getInstance().token.await()
                val success = uploadTokenToServer(account.token, getDeviceId(), getDeviceName(), realToken)
                if (success) {
                    Log.d(TAG, "✅ Token synced for new login: ${account.email}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync token for new login: ${e.message}")
            }
        }
    }

    private fun uploadTokenToServer(jwtToken: String, deviceId: String, deviceName: String, fcmToken: String): Boolean {
        return try {
            val jsonObject = JSONObject().apply {
                put("deviceId", deviceId)
                put("deviceName", deviceName)
                put("fcmToken", fcmToken)
            }

            val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(BACKEND_URL)
                .addHeader("Authorization", "Bearer $jwtToken")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val isSuccess = response.isSuccessful

            if (!isSuccess) {
                Log.e(TAG, "Server rejected token update. HTTP Code: ${response.code}")
            }

            response.close()
            isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Network exception uploading token: ${e.message}")
            false
        }
    }

    private fun saveTokenLocally(token: String) {
        sharedPreferences.edit().putString(SAVED_TOKEN_KEY, token).apply()
    }
}