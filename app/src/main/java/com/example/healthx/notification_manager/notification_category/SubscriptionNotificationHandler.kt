package com.example.healthx.notification_manager.notification_category

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
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

    suspend fun handleNotification(
        notificationIdString: String,
        title: String,
        smallDescription: String,
        imageUrl: String?,
        subscriptionId: String? // Unique to this category
    ) {
        val uniqueIntId = notificationIdString.hashCode()

        // 1. Intent for Body Tap (Opens Main List View - NO Subscription ID passed)
        val bodyTapIntent = Intent(context, SubscriptionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val bodyTapPendingIntent = PendingIntent.getActivity(
            context,
            uniqueIntId,
            bodyTapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Intent for "Subscribe" Button (Opens Detail View - PASSES Subscription ID)
        val subscribeIntent = Intent(context, SubscriptionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_SUBSCRIPTION_ID", subscriptionId)
        }
        val subscribePendingIntent = PendingIntent.getActivity(
            context,
            uniqueIntId + 1, // Differentiate pending intent request code
            subscribeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Intent for "Ignore" Button (Dismisses notification silently via BroadcastReceiver)
        val ignoreIntent = Intent(context, SubscriptionActionReceiver::class.java).apply {
            action = SubscriptionActionReceiver.ACTION_IGNORE_SUBSCRIPTION
            putExtra(SubscriptionActionReceiver.EXTRA_NOTIFICATION_ID, uniqueIntId)
        }
        val ignorePendingIntent = PendingIntent.getBroadcast(
            context,
            uniqueIntId + 2,
            ignoreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Download Image if present
        val imageBitmap = imageUrl?.let { downloadImage(it) }

        // Build Notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_default_notification) // Replace with your subscription icon
            .setContentTitle(title)
            .setContentText(smallDescription)
            .setAutoCancel(true)
            .setContentIntent(bodyTapPendingIntent) // Action for tapping the notification body
            .setGroup(GROUP_SUBSCRIPTION)
            // Add the action buttons
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

        notificationManager.notify(uniqueIntId, builder.build())
    }

    private suspend fun downloadImage(urlStr: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlStr)
            BitmapFactory.decodeStream(url.openConnection().inputStream)
        } catch (e: Exception) {
            null
        }
    }
}