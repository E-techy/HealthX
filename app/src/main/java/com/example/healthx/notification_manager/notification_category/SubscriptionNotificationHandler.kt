package com.example.healthx.notification_manager.notification_category

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.healthx.R
import com.example.healthx.ui.subscription.SubscriptionActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class SubscriptionNotificationHandler(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "healthx_subscription_channel"
        const val GROUP_SUBSCRIPTION = "group_subscription"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Subscriptions & Payments"
            val descriptionText = "Notifications regarding your HealthX Pro plans and billing"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
            Log.d("FCM_DEBUG", "HANDLER: Notification Channel created/verified")
        }
    }

    suspend fun handleNotification(
        notificationIdString: String,
        title: String,
        smallDescription: String,
        imageUrl: String?,
        subscriptionId: String?
    ) {
        Log.d("FCM_DEBUG", "HANDLER: Starting to build notification")

        // CRITICAL CHECK: Are notifications actually enabled for this app?
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.e("FCM_DEBUG", "❌ CRITICAL ERROR: The OS has blocked notifications for this app! Go to App Settings and enable them.")
        }

        val uniqueIntId = notificationIdString.hashCode()

        val bodyTapIntent = Intent(context, SubscriptionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val bodyTapPendingIntent = PendingIntent.getActivity(
            context, uniqueIntId, bodyTapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subscribeIntent = Intent(context, SubscriptionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_SUBSCRIPTION_ID", subscriptionId)
        }
        val subscribePendingIntent = PendingIntent.getActivity(
            context, uniqueIntId + 1, subscribeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ignoreIntent = Intent(context, SubscriptionActionReceiver::class.java).apply {
            action = SubscriptionActionReceiver.ACTION_IGNORE_SUBSCRIPTION
            putExtra(SubscriptionActionReceiver.EXTRA_NOTIFICATION_ID, uniqueIntId)
        }
        val ignorePendingIntent = PendingIntent.getBroadcast(
            context, uniqueIntId + 2, ignoreIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d("FCM_DEBUG", "HANDLER: Downloading image if URL exists: $imageUrl")
        val imageBitmap = imageUrl?.let { downloadImage(it) }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_default_notification)
            .setContentTitle(title)
            .setContentText(smallDescription)
            .setAutoCancel(true)
            .setContentIntent(bodyTapPendingIntent)
            .setGroup(GROUP_SUBSCRIPTION)
            .addAction(R.drawable.ic_default_notification, "Ignore", ignorePendingIntent)
            .addAction(R.drawable.ic_default_notification, "Subscribe", subscribePendingIntent)

        if (imageBitmap != null) {
            builder.setLargeIcon(imageBitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(imageBitmap)
                    .bigLargeIcon(null as Bitmap?)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(smallDescription))
        }

        try {
            notificationManager.notify(uniqueIntId, builder.build())
            Log.d("FCM_DEBUG", "HANDLER: notificationManager.notify() called successfully! Pop-up should be visible.")
        } catch (e: SecurityException) {
            Log.e("FCM_DEBUG", "❌ SECURITY EXCEPTION: Android 13+ blocked the notification. Missing POST_NOTIFICATIONS runtime permission.")
        } catch (e: Exception) {
            Log.e("FCM_DEBUG", "❌ ERROR calling notify(): ${e.message}")
        }
    }

    private suspend fun downloadImage(urlStr: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlStr)
            BitmapFactory.decodeStream(url.openConnection().inputStream)
        } catch (e: Exception) {
            Log.e("FCM_DEBUG", "HANDLER: Failed to download image: ${e.message}")
            null
        }
    }
}