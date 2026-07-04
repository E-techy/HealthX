package com.example.healthx.alarm_manager

import android.app.Application
import android.content.Intent
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

    // --- STATE FLOWS ---

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
            activeAlarms.collect { pending ->
                _allAlarms.value = pending
            }
        }
    }

    // --- ACTIONS ---

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
        // If the alarm is ringing right now (or about to within 1 min), kill the service first
        val isRunning = alarm.status == "PENDING" && (alarm.triggerTimeMillis - System.currentTimeMillis() < 60_000)
        if (isRunning) {
            stopRunningAlarm(alarm.id)
        }

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

    // --- FAKE SERVER APIs ---

    fun syncAlarmWithCloud(alarmId: Int) {
        viewModelScope.launch { delay(1500) }
    }

    fun downloadCloudMediaForAlarm(cloudUrl: String) {
        viewModelScope.launch { delay(2000) }
    }

    // --- UTILS ---

    fun formatTriggerTime(timeMillis: Long): String {
        val formatter = SimpleDateFormat("EEE, MMM dd • hh:mm a", Locale.getDefault())
        return formatter.format(Date(timeMillis))
    }
}