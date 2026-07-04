package com.example.healthx.alarm_manager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.healthx.R
import com.example.healthx.data.local.AppDatabase
import com.example.healthx.data.local.entities.AlarmEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

class MediaForegroundService : Service(), TextToSpeech.OnInitListener {

    private val TAG = "MediaService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null

    private lateinit var audioManager: AudioManager
    private var originalVolume: Int = -1

    companion object {
        const val CHANNEL_ID = "healthx_alarm_channel"
        const val NOTIFICATION_ID = 9999 // Fixed ID so we only show one alarm at a time
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        tts = TextToSpeech(this, this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: return START_NOT_STICKY

        // 1. Acquire Wakelock & Wake Screen
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "HealthX::AlarmWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L /* 10 minutes max */)

        // 2. Fetch Alarm Details
        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val alarm = db.alarmDao().getAlarmById(alarmId)

            if (alarm != null) {
                // 3. Show Un-swipeable Notification
                startForeground(NOTIFICATION_ID, buildNotification(alarm))

                // 4. Override Volume
                originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                // Assuming alarm.volumeLevel exists. Defaulting to max if not.
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)

                // 5. Play Audio
                playAudio(alarm)
            } else {
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun playAudio(alarm: AlarmEntity) {
        when (alarm.audioPlaybackType) {
            "LOCAL_FILE" -> playMediaPlayer(alarm.localAudioUri)
            "CLOUD_MEDIA" -> {
                playMediaPlayer(alarm.cloudMediaUrl)
            }
            "TTS" -> {
                // FIX: Use elvis operator to handle the nullable Int result safely
                val languageResult = tts?.isLanguageAvailable(Locale.US) ?: TextToSpeech.LANG_NOT_SUPPORTED

                if (languageResult >= TextToSpeech.LANG_AVAILABLE) {
                    tts?.speak(alarm.ttsContent ?: "Alarm triggering", TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
                } else {
                    playDefaultFallback()
                }
            }
            else -> playDefaultFallback()
        }
    }

    private fun playMediaPlayer(uri: String?) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                setDataSource(uri ?: throw Exception("URI is null"))
                isLooping = true
                prepareAsync()
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, _, _ ->
                    playDefaultFallback()
                    true
                }
            }
        } catch (e: Exception) {
            playDefaultFallback()
        }
    }

    private fun playDefaultFallback() {
        Log.e(TAG, "Playing default system fallback audio.")
        // Play a raw resource file bundled with your app
        // mediaPlayer = MediaPlayer.create(this, R.raw.default_alarm_sound)
        // mediaPlayer?.isLooping = true
        // mediaPlayer?.start()
    }

    private fun buildNotification(alarm: AlarmEntity): Notification {
        // Stop Action
        val stopIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = "ACTION_STOP"
            putExtra("ALARM_ID", alarm.id)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(this, alarm.id, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Snooze Action
        val snoozeIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("ALARM_ID", alarm.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(this, alarm.id + 1, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Empty Full Screen Intent to wake screen on locked devices
        val fullScreenIntent = Intent(this, AlarmReceiver::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(this, 0, fullScreenIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(alarm.title)
            .setContentText(alarm.description)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true) // CRITICAL: Makes it un-swipeable
            .setFullScreenIntent(fullScreenPendingIntent, true) // Wakes screen
            .addAction(android.R.drawable.ic_media_pause, "Snooze (5m)", snoozePendingIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "HealthX Alarms", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setBypassDnd(true)
                description = "Critical Health Alarms"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up locks and audio
        mediaPlayer?.stop()
        mediaPlayer?.release()
        tts?.stop()
        tts?.shutdown()

        // Restore user's original volume
        if (originalVolume != -1) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
        }

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}