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

class HealthXMessagingService : FirebaseMessagingService() {

    // A Coroutine Scope specifically for this service to run database/download tasks in the background
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Firebase payloads are delivered in the 'data' map
        val data = remoteMessage.data

        if (data.isNotEmpty()) {
            val categoryString = data["category"] ?: return

            // Route the notification based on its category
            serviceScope.launch {
                try {
                    val category = NotificationCategory.valueOf(categoryString)
                    routeNotification(category, data)
                } catch (e: IllegalArgumentException) {
                    Log.e("FCM", "Unknown notification category received: $categoryString")
                } catch (e: Exception) {
                    Log.e("FCM", "Error processing notification: ${e.message}")
                }
            }
        }
    }

    private suspend fun routeNotification(category: NotificationCategory, data: Map<String, String>) {
        val notificationId = data["notificationId"] ?: System.currentTimeMillis().toString()
        val title = data["title"] ?: "HealthX"
        val smallDescription = data["smallDescription"] ?: ""
        val fullDescription = data["fullDescription"]
        val imageUrl = data["imageUrl"]

        when (category) {
            // ==========================================
            // 1. COMMON NOTIFICATIONS LOOP
            // ==========================================
            NotificationCategory.OTP,
            NotificationCategory.NEW_DEVICE_REGISTERED,
            NotificationCategory.NEW_AI_CHAT_RECEIVED,
            NotificationCategory.ADVERTISEMENT -> {

                val deepLinkUrl = data["deepLinkUrl"]
                // Default expiry to 7 days if not provided
                val expiryTimeEpoch = data["expiryTimeEpoch"]?.toLongOrNull() ?: (System.currentTimeMillis() + 604800000L)

                // 1a. Initialize local dependencies
                val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
                val repository = NotificationRepository(dao)
                val displayManager = NotificationDisplayManager(applicationContext)

                // 1b. Save to Room Database FIRST
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

                // 1c. Trigger the physical pop-up
                displayManager.handleInitialNotification(
                    notificationId = notificationId,
                    category = category.name,
                    title = title,
                    smallDescription = smallDescription,
                    imageUrl = imageUrl
                )
            }

            // ==========================================
            // 2. SUBSCRIPTION NOTIFICATION LOOP
            // ==========================================
            NotificationCategory.SUBSCRIPTION -> {

                val subscriptionId = data["subscriptionId"] // Unique payload for this category

                val subscriptionHandler = SubscriptionNotificationHandler(applicationContext)

                // Trigger the specialized subscription pop-up (with Subscribe/Ignore buttons)
                subscriptionHandler.handleNotification(
                    notificationIdString = notificationId,
                    title = title,
                    smallDescription = smallDescription,
                    imageUrl = imageUrl,
                    subscriptionId = subscriptionId
                )
            }

            // ==========================================
            // 3. FUTURE LOOPS (Payments, Reminders, etc.)
            // ==========================================
            else -> {
                Log.d("FCM", "Category ${category.name} routing not yet implemented.")
            }
        }
    }

    /**
     * Called whenever a new FCM token is generated for the device.
     * You should send this token to your Node.js backend so it knows where to send pushes.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Token: $token")
        // TODO: Send this token to your backend via an API call (e.g., api/users/update-fcm-token)
    }
}