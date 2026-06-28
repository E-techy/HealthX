package com.example.healthx.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.healthx.data.local.converters.ReminderTypeConverters
import com.example.healthx.data.local.dao.ReminderDao
import com.example.healthx.data.local.entities.ReminderEntity
import com.example.healthx.notification_manager.NotificationDao
import com.example.healthx.notification_manager.NotificationEntity

@Database(
    entities = [
        ReminderEntity::class,       // Your existing reminder table
        NotificationEntity::class    // ADDED: The new notification table
    ],
    version = 2, // ADDED: Bumped version up to trigger schema update
    exportSchema = false
)
@TypeConverters(ReminderTypeConverters::class) // Your existing converters
abstract class AppDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    // ADDED: Expose the notification DAO so the repository can use it
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "healthx_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}