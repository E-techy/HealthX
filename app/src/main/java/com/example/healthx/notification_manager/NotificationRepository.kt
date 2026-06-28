package com.example.healthx.notification_manager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationRepository(private val notificationDao: NotificationDao) {

    /**
     * Saves the incoming server payload to the local Room database.
     * This must run before we trigger the system UI pop-up.
     */
    suspend fun saveCommonNotification(
        notificationId: String,
        category: String,
        title: String,
        smallDescription: String,
        fullDescription: String?,
        imageUrl: String?,
        deepLinkUrl: String?,
        expiryTimeEpoch: Long
    ) {
        withContext(Dispatchers.IO) {
            val entity = NotificationEntity(
                notificationId = notificationId,
                category = category,
                title = title,
                smallDescription = smallDescription,
                fullDescription = fullDescription,
                imageUrl = imageUrl,
                deepLinkUrl = deepLinkUrl,
                expiryTimeEpoch = expiryTimeEpoch
            )

            notificationDao.insertNotification(entity)
        }
    }
}