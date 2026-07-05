package com.example.healthx.alarm_manager

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthx.data.local.AppDatabase
import com.example.healthx.data.local.entities.AlarmEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AlarmManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val alarmDao = db.alarmDao()

    val activeAlarms: StateFlow<List<AlarmEntity>> = alarmDao.getPendingAlarmsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _allAlarms = MutableStateFlow<List<AlarmEntity>>(emptyList())
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    val filteredAllAlarms = combine(_allAlarms, _selectedCategoryFilter) { alarms, category ->
        if (category == null) alarms else alarms.filter { it.category == category }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            activeAlarms.collect { pending -> _allAlarms.value = pending }
        }
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
    }

    fun stopRunningAlarm(alarmId: Int) {
        val intent = Intent(getApplication(), AlarmActionReceiver::class.java).apply {
            action = "ACTION_STOP"
            putExtra("ALARM_ID", alarmId)
        }
        getApplication<Application>().sendBroadcast(intent)
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        val isRunning = alarm.status == "PENDING" && (alarm.triggerTimeMillis - System.currentTimeMillis() < 60_000)
        if (isRunning) stopRunningAlarm(alarm.id)

        viewModelScope.launch {
            alarmDao.deleteAlarm(alarm)
            alarmDao.removeFromLedger(alarm.id)
            RollingScheduleEngine(getApplication()).updateOSAlarms()
        }
    }

    fun updateAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            alarmDao.updateAlarm(alarm)
            RollingScheduleEngine(getApplication()).updateOSAlarms()
        }
    }

    /**
     * Translates the UI selections into the DB entity and kicks the OS engine.
     */
    fun createAlarm(
        title: String,
        description: String,
        category: String,
        triggerTimeMillis: Long, // Computed from Calendar+Time dials in the UI
        volumeLevel: Float,      // 0.0 to 1.0
        audioType: String,
        localUri: String?,
        ttsContent: String?,
        cloudUrl: String?,
        isRecurring: Boolean,
        recurrenceType: String?,
        recurrenceInterval: Int?
    ) {
        val newAlarm = AlarmEntity(
            remoteId = null,
            triggerTimeMillis = triggerTimeMillis,
            category = category,
            logoUrl = null,
            audioPlaybackType = audioType,
            localAudioUri = localUri,
            ttsContent = ttsContent,
            cloudMediaUrl = cloudUrl,
            status = "PENDING",
            title = title,
            description = description,
            isRecurring = isRecurring,
            recurrenceType = recurrenceType,
            recurrenceInterval = recurrenceInterval,
            recurrenceStartDate = if (isRecurring) triggerTimeMillis else null,
            recurrenceEndDate = null
        )

        viewModelScope.launch {
            alarmDao.insertAlarm(newAlarm)
            RollingScheduleEngine(getApplication()).updateOSAlarms()
        }
    }

    fun takePersistableUriPermission(uri: Uri) {
        try {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            getApplication<Application>().contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun syncAlarmWithCloud(alarmId: Int) { viewModelScope.launch { delay(1500) } }
    fun downloadCloudMediaForAlarm(cloudUrl: String) { viewModelScope.launch { delay(2000) } }

    fun formatTriggerTime(timeMillis: Long): String {
        val formatter = SimpleDateFormat("EEE, MMM dd • hh:mm a", Locale.getDefault())
        return formatter.format(Date(timeMillis))
    }
}