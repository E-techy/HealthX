package com.example.healthx.ui.components.reminders.core

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.healthx.data.models.reminders.core.AlarmConfig
import com.example.healthx.data.models.reminders.core.AudioType

@Composable
fun AlarmConfigBlock(
    initialConfig: AlarmConfig = AlarmConfig(),
    onConfigChanged: (AlarmConfig) -> Unit
) {
    // Local state for the UI to react instantly
    var audioType by remember { mutableStateOf(initialConfig.audioType) }
    var isVibrationEnabled by remember { mutableStateOf(initialConfig.isVibrationEnabled) }
    var volumeLevel by remember { mutableStateOf(initialConfig.volumeLevel) }

    // Helper to fire the callback whenever anything changes
    fun updateParent() {
        onConfigChanged(
            AlarmConfig(
                audioType = audioType,
                isVibrationEnabled = isVibrationEnabled,
                volumeLevel = volumeLevel,
                localAudioUri = initialConfig.localAudioUri,
                cloudAudioUrl = initialConfig.cloudAudioUrl
            )
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Alarm Settings", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))

            // Audio Type Selector
            Text("Tone Type", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AudioType.values().forEach { type ->
                    val isSelected = audioType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            audioType = type
                            updateParent()
                        },
                        label = { Text(type.name.replace("_", " ")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vibration Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Vibrate when alarm rings", color = Color.White)
                Switch(
                    checked = isVibrationEnabled,
                    onCheckedChange = {
                        isVibrationEnabled = it
                        updateParent()
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Volume Slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Slider(
                    value = volumeLevel,
                    onValueChange = {
                        volumeLevel = it
                        updateParent()
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}