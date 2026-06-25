package com.example.healthx.reminders.scheduler

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.healthx.R

class AlarmRingingService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private val CHANNEL_ID = "HEALTHX_ALARM_CHANNEL"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == "ACTION_STOP") {
            stopAlarm()
            return START_NOT_STICKY
        }

        if (action == "ACTION_SNOOZE") {
            snoozeAlarm(intent)
            return START_NOT_STICKY
        }

        // 1. Extract Data
        val title = intent?.getStringExtra("TITLE") ?: "HealthX Alarm"
        val desc = intent?.getStringExtra("DESCRIPTION") ?: "Time for your reminder"
        val category = intent?.getStringExtra("CATEGORY") ?: "Reminder"
        val audioUriString = intent?.getStringExtra("AUDIO_URI")

        // 2. Start Notification
        createNotificationChannel()
        val notification = buildNotification(title, desc, category, intent)
        startForeground(1001, notification)

        // 3. Play Music
        playAudio(audioUriString)

        return START_STICKY
    }

    private fun buildNotification(title: String, desc: String, category: String, originalIntent: Intent?): Notification {
        // Stop Action
        val stopIntent = Intent(this, AlarmRingingService::class.java).apply { action = "ACTION_STOP" }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        // Snooze Action (5 mins)
        val snoozeIntent = Intent(this, AlarmRingingService::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtras(originalIntent?.extras ?: return@apply)
        }
        val snoozePendingIntent = PendingIntent.getService(this, 1, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Replace with your app icon
            .setContentTitle("[$category] $title")
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Snooze (5m)", snoozePendingIntent)
            .build()
    }

    private fun playAudio(audioUriString: String?) {
        try {
            mediaPlayer = MediaPlayer().apply {
                // ALWAYS default to the system alarm if string is null or empty
                val alarmUri = if (!audioUriString.isNullOrBlank()) {
                    Uri.parse(audioUriString)
                } else {
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                }

                setDataSource(this@AlarmRingingService, alarmUri)
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Final fallback: try to use notification sound if alarm fails
            val fallbackUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer?.setDataSource(this, fallbackUri)
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        }
    }
    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun snoozeAlarm(intent: Intent?) {
        stopAlarm() // Stop the ringing

        // Reschedule for 5 minutes (300,000 ms) from now
        val alarmManager = getSystemService(AlarmManager::class.java)
        val snoozeTime = System.currentTimeMillis() + (5 * 60 * 1000)

        val newIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtras(intent?.extras ?: return@apply)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, newIntent.getStringExtra("REMINDER_ID").hashCode(), newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Alarms", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for ringing alarms"
            setSound(null, null) // Sound is handled by MediaPlayer, not the notification!
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }
}