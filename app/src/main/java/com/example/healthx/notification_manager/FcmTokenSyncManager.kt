package com.example.healthx.notification_manager

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * FcmTokenSyncManager handles the synchronization of the Firebase Cloud Messaging (FCM) token
 * between the Android device and the HealthX backend server.
 *
 * Example Usage:
 * ```
 * val syncManager = FcmTokenSyncManager(context)
 * * // 1. ONLINE Mode (Force Sync - Good for fresh logins)
 * syncManager.syncToken(
 * backendUrl = "[http://10.0.2.2:5001/api/device/token](http://10.0.2.2:5001/api/device/token)",
 * jwtToken = "eyJhbGciOi...",
 * deviceId = "android_test_device_001",
 * deviceName = "Samsung Galaxy S23",
 * syncMode = SyncMode.ONLINE
 * )
 * * // 2. OFFLINE Mode (Smart/Lazy Sync - Good for app startup)
 * // Only sends a network request if the token has actually changed since the last successful sync.
 * syncManager.syncToken(
 * backendUrl = "[http://10.0.2.2:5001/api/device/token](http://10.0.2.2:5001/api/device/token)",
 * jwtToken = "eyJhbGciOi...",
 * deviceId = "android_test_device_001",
 * deviceName = "Samsung Galaxy S23",
 * syncMode = SyncMode.OFFLINE
 * )
 * ```
 */
class FcmTokenSyncManager(private val context: Context) {

    private val TAG = "FcmTokenSync"
    private val client = OkHttpClient()

    // Local storage to keep track of the token that is currently mirrored in the cloud
    private val sharedPreferences = context.getSharedPreferences("HealthX_FCM_Prefs", Context.MODE_PRIVATE)
    private val SAVED_TOKEN_KEY = "CLOUD_MIRRORED_FCM_TOKEN"

    enum class SyncMode {
        ONLINE,  // Forces a network request to the backend every time
        OFFLINE  // Compares local vs real token; only networks if they don't match
    }

    /**
     * Syncs the FCM token to the backend server based on the provided mode.
     * Wrapped entirely in a try-catch to ensure it is completely safe to call from anywhere.
     */
    suspend fun syncToken(
        backendUrl: String,
        jwtToken: String,
        deviceId: String,
        deviceName: String,
        syncMode: SyncMode = SyncMode.ONLINE
    ) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Fetch the real, current FCM token from Firebase
                val realToken = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "Real FCM Token fetched: $realToken")

                when (syncMode) {
                    SyncMode.ONLINE -> {
                        // Always send to the server
                        Log.d(TAG, "ONLINE Mode: Forcing token upload to server.")
                        val success = uploadTokenToServer(backendUrl, jwtToken, deviceId, deviceName, realToken)
                        if (success) {
                            saveTokenLocally(realToken)
                        }
                    }

                    SyncMode.OFFLINE -> {
                        // Smart check to save bandwidth/database load
                        val locallySavedToken = sharedPreferences.getString(SAVED_TOKEN_KEY, null)

                        if (locallySavedToken == null || locallySavedToken != realToken) {
                            Log.d(TAG, "OFFLINE Mode: Mismatch detected. Local: $locallySavedToken, Real: $realToken. Uploading...")
                            val success = uploadTokenToServer(backendUrl, jwtToken, deviceId, deviceName, realToken)

                            // ONLY update the local representation if the server successfully received it (200 OK)
                            if (success) {
                                saveTokenLocally(realToken)
                                Log.d(TAG, "OFFLINE Mode: Sync successful. Local token updated.")
                            } else {
                                Log.e(TAG, "OFFLINE Mode: Sync failed. Local token NOT updated.")
                            }
                        } else {
                            Log.d(TAG, "OFFLINE Mode: Tokens match. No network request required.")
                        }
                    }
                }
            } catch (e: Exception) {
                // Catch-all for Firebase fetching errors or coroutine cancellations
                Log.e(TAG, "Critical error during FCM token sync execution: ${e.message}")
            }
        }
    }

    /**
     * Private helper to execute the HTTP POST request to the backend.
     * Returns true if the server responds with a 200-299 success code.
     */
    private fun uploadTokenToServer(
        url: String,
        jwtToken: String,
        deviceId: String,
        deviceName: String,
        fcmToken: String
    ): Boolean {
        return try {
            // Build the JSON Payload
            val jsonObject = JSONObject().apply {
                put("deviceId", deviceId)
                put("deviceName", deviceName)
                put("fcmToken", fcmToken)
            }

            val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

            // Build the Request
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $jwtToken")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            // Execute the Request
            val response = client.newCall(request).execute()
            val isSuccess = response.isSuccessful

            response.close() // Always close the response to prevent memory leaks

            isSuccess
        } catch (e: Exception) {
            // Broadened exception catch to handle IOException (Network) or JSONException
            Log.e(TAG, "Exception while building or uploading token: ${e.message}")
            false
        }
    }

    /**
     * Updates the locally permanent SharedPreferences with the token that was just synced.
     */
    private fun saveTokenLocally(token: String) {
        sharedPreferences.edit().putString(SAVED_TOKEN_KEY, token).apply()
    }
}