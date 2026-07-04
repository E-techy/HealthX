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
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.healthx.data.local.AppDatabase
import com.example.healthx.data.local.entities.AlarmEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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

    // TTS State Tracking
    private var isTtsReady = false
    private var ttsTextToLoop: String? = null
    private var shouldLoopTts = true // Safety flag to kill loop on destroy

    companion object {
        const val CHANNEL_ID = "healthx_alarm_channel"
        const val NOTIFICATION_ID = 9999
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        tts = TextToSpeech(this, this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: return START_NOT_STICKY

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "HealthX::AlarmWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L)

        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val alarm = db.alarmDao().getAlarmById(alarmId)

            if (alarm != null) {
                startForeground(NOTIFICATION_ID, buildNotification(alarm))

                originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)

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
            "CLOUD_MEDIA" -> playMediaPlayer(alarm.cloudMediaUrl)
            "TTS" -> {
                ttsTextToLoop = alarm.ttsContent ?: "Alarm triggering"
                if (isTtsReady) {
                    startTtsLoop()
                }
            }
            else -> playDefaultFallback()
        }
    }

    private fun startTtsLoop() {
        val text = ttsTextToLoop ?: return
        if (!shouldLoopTts) return

        // Use a Bundle to map the Utterance ID for the progress listener
        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "TTS_ID")

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "TTS_ID")
    }

    private fun playMediaPlayer(uriString: String?) {
        if (uriString.isNullOrBlank()) {
            playDefaultFallback()
            return
        }

        try {
            val uri = Uri.parse(uriString)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(applicationContext, uri)
                isLooping = true
                prepareAsync()
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: $what, $extra")
                    playDefaultFallback()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load media: ${e.message}")
            playDefaultFallback()
        }
    }

    private fun playDefaultFallback() {
        Log.e(TAG, "Playing default system fallback audio.")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true

                // Attach the listener to handle the infinite loop
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onError(utteranceId: String?) {}

                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == "TTS_ID" && shouldLoopTts) {
                            // Wait 2 seconds before repeating so it sounds natural
                            serviceScope.launch {
                                delay(2000)
                                if (shouldLoopTts) {
                                    startTtsLoop()
                                }
                            }
                        }
                    }
                })

                // If an alarm triggered while TTS was booting up, play it now
                if (ttsTextToLoop != null) {
                    startTtsLoop()
                }
            }
        }
    }

    private fun buildNotification(alarm: AlarmEntity): Notification {
        val stopIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = "ACTION_STOP"
            putExtra("ALARM_ID", alarm.id)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(this, alarm.id, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val snoozeIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("ALARM_ID", alarm.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(this, alarm.id + 1, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val fullScreenIntent = Intent(this, AlarmReceiver::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(this, 0, fullScreenIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(alarm.title)
            .setContentText(alarm.description)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
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
        shouldLoopTts = false // Immediately kill the loop flag

        mediaPlayer?.stop()
        mediaPlayer?.release()

        tts?.stop()
        tts?.shutdown()

        if (originalVolume != -1) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
        }
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}