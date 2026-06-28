package com.example.healthx.notification_manager

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.example.healthx.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class NotificationDisplayManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val GROUP_AI_CHAT = "group_ai_chat"
        const val GROUP_SECURITY = "group_security"
        const val GROUP_PROMO = "group_promo"
        const val CHANNEL_ID = "healthx_common_channel"
    }

    suspend fun handleInitialNotification(
        notificationId: String,
        category: String,
        title: String,
        smallDescription: String,
        imageUrl: String?
    ) {
        val (groupKey, smallIconRes) = determineGroupAndIcon(category)

        // Note: Make sure CommonNotificationActivity exists in your project
        // Or update this intent to point to the correct Activity that hosts your Composable
        val intent = Intent().setClassName(context, "com.example.healthx.ui.CommonNotificationActivity").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val imageBitmap = imageUrl?.let { downloadImage(it) }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconRes)
            .setContentTitle(title)
            .setContentText(smallDescription)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(groupKey)

        if (imageBitmap != null) {
            builder.setLargeIcon(imageBitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(imageBitmap)
                    .bigLargeIcon(null as Bitmap?)
            )
        } else {
            builder.setStyle(
                NotificationCompat.BigTextStyle().bigText(smallDescription)
            )
        }

        val uniqueIntId = notificationId.hashCode()
        notificationManager.notify(uniqueIntId, builder.build())

        showGroupSummary(groupKey, smallIconRes)
    }

    private fun showGroupSummary(groupKey: String, iconRes: Int) {
        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("HealthX")
            .setSmallIcon(iconRes)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(groupKey.hashCode(), summaryNotification)
    }

    private fun determineGroupAndIcon(category: String): Pair<String, Int> {
        return when (category) {
            NotificationCategory.NEW_AI_CHAT_RECEIVED.name ->
                Pair(GROUP_AI_CHAT, R.drawable.ic_ai_chat) // Ensure you have this drawable

            NotificationCategory.OTP.name,
            NotificationCategory.NEW_DEVICE_REGISTERED.name ->
                Pair(GROUP_SECURITY, R.drawable.ic_security_alert) // Ensure you have this drawable

            NotificationCategory.ADVERTISEMENT.name ->
                Pair(GROUP_PROMO, R.drawable.ic_promo_ad) // Ensure you have this drawable

            else -> Pair("group_default", R.drawable.ic_default_notification) // Ensure you have this drawable
        }
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