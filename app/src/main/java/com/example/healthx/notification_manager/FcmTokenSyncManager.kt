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

    private val TAG = "FCM_SYNC_DEBUG"
    private val sessionManager = SessionManager(context)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val sharedPreferences = context.getSharedPreferences("HealthX_FCM_Prefs", Context.MODE_PRIVATE)
    private val SAVED_TOKEN_KEY = "CLOUD_MIRRORED_FCM_TOKEN"
    private val DEVICE_ID_KEY = "HEALTHX_DEVICE_ID"

    // Verify this URL matches your Node.js route!
    private val BACKEND_URL = "${BuildConfig.BASE_URL}device/sync-token"

    enum class SyncMode { ONLINE, OFFLINE }

    fun getDeviceId(): String {
        var deviceId = sharedPreferences.getString(DEVICE_ID_KEY, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            sharedPreferences.edit().putString(DEVICE_ID_KEY, deviceId).apply()
        }
        return deviceId
    }

    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.lowercase().startsWith(manufacturer.lowercase())) {
            model.replaceFirstChar { it.uppercase() }
        } else {
            "${manufacturer.replaceFirstChar { it.uppercase() }} $model"
        }
    }

    suspend fun syncAllAccounts(syncMode: SyncMode = SyncMode.ONLINE) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "--------------------------------------------------")
                Log.d(TAG, "1. Starting Token Sync Process. Mode: $syncMode")

                Log.d(TAG, "2. Requesting token from Firebase...")
                val realToken = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "3. Firebase returned token: ${realToken.take(15)}...")

                val locallySavedToken = sharedPreferences.getString(SAVED_TOKEN_KEY, null)

                if (syncMode == SyncMode.OFFLINE && locallySavedToken == realToken) {
                    Log.d(TAG, "4. [ABORT] Token matches local cache. No network needed.")
                    return@withContext
                }

                val accounts = sessionManager.savedAccountsFlow.first()
                Log.d(TAG, "4. Found ${accounts.size} logged-in account(s) locally.")

                if (accounts.isEmpty()) {
                    Log.d(TAG, "5. [ABORT] No accounts to sync with. Caching token.")
                    saveTokenLocally(realToken)
                    return@withContext
                }

                val deviceId = getDeviceId()
                val deviceName = getDeviceName()
                var allSyncsSuccessful = true

                for (account in accounts) {
                    if (account.isGuest) {
                        Log.d(TAG, "   -> Skipping Guest Account")
                        continue
                    }

                    Log.d(TAG, "5. Preparing POST request for account: ${account.email}")
                    val success = uploadTokenToServer(account.token, deviceId, deviceName, realToken)

                    if (!success) {
                        Log.e(TAG, "❌ Failed to sync token for account: ${account.email}")
                        allSyncsSuccessful = false
                    } else {
                        Log.d(TAG, "✅ Success for account: ${account.email}")
                    }
                }

                if (allSyncsSuccessful) {
                    saveTokenLocally(realToken)
                    Log.d(TAG, "6. 🎉 All accounts synced. Local cache updated.")
                } else {
                    Log.w(TAG, "6. ⚠️ Sync incomplete. Will retry next launch.")
                }
                Log.d(TAG, "--------------------------------------------------")

            } catch (e: Exception) {
                Log.e(TAG, "🔥 CRITICAL ERROR during sync: ${e.message}")
            }
        }
    }

    suspend fun syncSingleAccount(account: SavedAccount) {
        withContext(Dispatchers.IO) {
            try {
                if (account.isGuest) return@withContext
                val realToken = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "Syncing single account immediately after auth: ${account.email}")
                val success = uploadTokenToServer(account.token, getDeviceId(), getDeviceName(), realToken)
                if (success) saveTokenLocally(realToken)
            } catch (e: Exception) {
                Log.e(TAG, "Failed single account sync: ${e.message}")
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

            Log.d(TAG, "   -> Hitting URL: $BACKEND_URL")
            val request = Request.Builder()
                .url(BACKEND_URL)
                .addHeader("Authorization", "Bearer $jwtToken")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val isSuccess = response.isSuccessful

            if (!isSuccess) {
                Log.e(TAG, "   ❌ HTTP Error: ${response.code} - ${response.body?.string()}")
            }

            response.close()
            isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "   ❌ Network Exception: ${e.message}")
            false
        }
    }

    private fun saveTokenLocally(token: String) {
        sharedPreferences.edit().putString(SAVED_TOKEN_KEY, token).apply()
    }
}