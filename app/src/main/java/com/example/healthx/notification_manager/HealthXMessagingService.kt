package com.example.healthx.notification_manager

import android.util.Log
import com.example.healthx.data.local.AppDatabase
import com.example.healthx.notification_manager.notification_category.SubscriptionNotificationHandler
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

class HealthXMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_DEBUG", "------------------------------------------------")
        Log.d("FCM_DEBUG", "1. onMessageReceived: PAYLOAD INTERCEPTED!")

        val data = remoteMessage.data

        if (data.isNotEmpty()) {
            Log.d("FCM_DEBUG", "2. Raw Data Payload: $data")

            // 1. Get the category from the correct top-level key your server is using
            val categoryString = data["notificationType"]

            if (categoryString == null) {
                Log.e("FCM_DEBUG", "❌ ERROR: No 'notificationType' found in top-level payload! Cannot route.")
                return
            }

            Log.d("FCM_DEBUG", "3. Category Identified: $categoryString")

            // 2. Extract and parse the nested payload string
            val payloadDataString = data["payloadData"] ?: "{}"
            val jsonPayload = try {
                JSONObject(payloadDataString)
            } catch (e: Exception) {
                Log.e("FCM_DEBUG", "❌ ERROR: Failed to parse payloadData as JSON: ${e.message}")
                JSONObject()
            }

            serviceScope.launch {
                try {
                    val category = NotificationCategory.valueOf(categoryString)
                    routeNotification(category, jsonPayload)
                } catch (e: IllegalArgumentException) {
                    Log.e("FCM_DEBUG", "❌ ERROR: Unknown category enum: $categoryString")
                } catch (e: Exception) {
                    Log.e("FCM_DEBUG", "❌ ERROR during routing: ${e.message}")
                    e.printStackTrace()
                }
            }
        } else {
            Log.e("FCM_DEBUG", "❌ ERROR: Payload data is empty!")
        }
    }

    // Notice we now pass a JSONObject instead of a Map<String, String>
    private suspend fun routeNotification(category: NotificationCategory, jsonPayload: JSONObject) {
        Log.d("FCM_DEBUG", "4. Routing Notification for category: ${category.name}")

        // optString safely extracts the value, or uses a default if the key is missing
        val notificationId = jsonPayload.optString("notificationId", System.currentTimeMillis().toString())
        val title = jsonPayload.optString("title", "HealthX")

        // Check for 'body' first, then 'smallDescription'
        var smallDescription = jsonPayload.optString("body", "")
        if (smallDescription.isEmpty()) {
            smallDescription = jsonPayload.optString("smallDescription", "")
        }

        val fullDescription = jsonPayload.optString("fullDescription", null)
        val imageUrl = jsonPayload.optString("imageUrl", null)

        Log.d("FCM_DEBUG", "5. Extracted Data -> Title: '$title', Desc: '$smallDescription'")

        when (category) {
            NotificationCategory.OTP,
            NotificationCategory.NEW_DEVICE_REGISTERED,
            NotificationCategory.NEW_AI_CHAT_RECEIVED,
            NotificationCategory.ADVERTISEMENT -> {
                Log.d("FCM_DEBUG", "6. Entering COMMON NOTIFICATION loop")
                val deepLinkUrl = jsonPayload.optString("deepLinkUrl", null)
                val expiryTimeEpoch = jsonPayload.optLong("expiryTimeEpoch", System.currentTimeMillis() + 604800000L)

                val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
                val repository = NotificationRepository(dao)
                val displayManager = NotificationDisplayManager(applicationContext)

                repository.saveCommonNotification(
                    notificationId = notificationId,
                    category = category.name,
                    title = title,
                    smallDescription = smallDescription,
                    fullDescription = fullDescription,
                    imageUrl = imageUrl,
                    deepLinkUrl = deepLinkUrl,
                    expiryTimeEpoch = expiryTimeEpoch
                )
                Log.d("FCM_DEBUG", "7. Saved to Room DB")

                displayManager.handleInitialNotification(
                    notificationId = notificationId,
                    category = category.name,
                    title = title,
                    smallDescription = smallDescription,
                    imageUrl = imageUrl
                )
                Log.d("FCM_DEBUG", "8. Passed to DisplayManager")
            }

            NotificationCategory.SUBSCRIPTION -> {
                Log.d("FCM_DEBUG", "6. Entering SUBSCRIPTION loop")

                // Extract subscription ID safely from the JSON object
                var subscriptionId = jsonPayload.optString("subscriptionDbId", "")
                if (subscriptionId.isEmpty()) {
                    subscriptionId = jsonPayload.optString("subscriptionId", "")
                }
                // Convert empty strings back to null if needed by your handler
                val finalSubId = if (subscriptionId.isNotEmpty()) subscriptionId else null

                Log.d("FCM_DEBUG", "7. Extracted Subscription ID: $finalSubId")

                val subscriptionHandler = SubscriptionNotificationHandler(applicationContext)

                subscriptionHandler.handleNotification(
                    notificationIdString = notificationId,
                    title = title,
                    smallDescription = smallDescription,
                    imageUrl = imageUrl,
                    subscriptionId = finalSubId
                )
                Log.d("FCM_DEBUG", "8. Passed to SubscriptionNotificationHandler")
            }

            else -> {
                Log.d("FCM_DEBUG", "❌ Category ${category.name} routing not yet implemented.")
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_DEBUG", "New Token: $token")
    }
}