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
import com.example.healthx.data.local.dao.AlarmDao
import com.example.healthx.data.local.entities.AlarmEntity

@Database(
    entities = [
        ReminderEntity::class,
        NotificationEntity::class,
        AlarmEntity::class           // ADDED: The new Alarm table
    ],
    version = 3,                     // ADDED: Bumped version from 2 to 3
    exportSchema = false
)
@TypeConverters(ReminderTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao
    abstract fun notificationDao(): NotificationDao
    abstract fun alarmDao(): AlarmDao  // ADDED: Expose the Alarm DAO

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
                    .fallbackToDestructiveMigration() // Wipes data on version bump during dev
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}