package com.example.healthx.notification_manager

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "common_notifications")
data class NotificationEntity(
    @PrimaryKey val notificationId: String,
    val category: String,
    val title: String,
    val smallDescription: String,
    val fullDescription: String?,
    val imageUrl: String?,
    val deepLinkUrl: String?,
    val expiryTimeEpoch: Long,
    val receivedAtEpoch: Long = System.currentTimeMillis(),
    var isRead: Boolean = false
)