package com.example.healthx.notification_manager

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    // Returns a reactive stream of notifications, excluding expired ones
    @Query("SELECT * FROM common_notifications WHERE expiryTimeEpoch > :currentTimeMillis ORDER BY receivedAtEpoch DESC")
    fun getActiveNotifications(currentTimeMillis: Long): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM common_notifications WHERE isRead = 0 AND expiryTimeEpoch > :currentTimeMillis")
    fun getUnreadCount(currentTimeMillis: Long): Flow<Int>

    @Query("UPDATE common_notifications SET isRead = 1 WHERE notificationId = :id")
    suspend fun markAsRead(id: String)

    @Query("DELETE FROM common_notifications WHERE expiryTimeEpoch <= :currentTimeMillis")
    suspend fun deleteExpiredNotifications(currentTimeMillis: Long)

    @Query("DELETE FROM common_notifications WHERE notificationId = :id")
    suspend fun deleteNotificationById(id: String)
}