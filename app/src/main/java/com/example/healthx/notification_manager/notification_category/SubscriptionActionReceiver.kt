package com.example.healthx.notification_manager.notification_category

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Handles background actions from the Subscription Notification (like the "Ignore" button).
 */
class SubscriptionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_IGNORE_SUBSCRIPTION) {
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
            if (notificationId != -1) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId) // Dismisses the pop-up
            }
        }
    }

    companion object {
        const val ACTION_IGNORE_SUBSCRIPTION = "com.example.healthx.ACTION_IGNORE_SUBSCRIPTION"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}