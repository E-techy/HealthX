package com.example.healthx.alarm_manager

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAlarmSection(viewModel: AlarmManagerViewModel, onCreated: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("MEDICAL") }

    // Time & Date State
    val calendar = Calendar.getInstance()
    var selectedTimeMillis by remember { mutableLongStateOf(calendar.timeInMillis) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Media & Volume
    var volume by remember { mutableFloatStateOf(1.0f) }
    var audioType by remember { mutableStateOf("TTS") }
    var ttsContent by remember { mutableStateOf("Please check your health schedule.") }
    var localAudioUri by remember { mutableStateOf<Uri?>(null) }
    var cloudUrl by remember { mutableStateOf("") }

    // Recurrence
    var isRecurring by remember { mutableStateOf(false) }
    var recurrenceType by remember { mutableStateOf("DAILY") }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            viewModel.takePersistableUriPermission(it)
            localAudioUri = it
            audioType = "LOCAL_FILE"
        }
    }

    Column(modifier = Modifier.padding(top = 16.dp)) {

        // --- 1. DATE & TIME PICKERS (ADVANCED UI) ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            // Date Selector
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f).height(72.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    Text("Date", color = Color.Gray, fontSize = 12.sp)
                    Text(SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date(selectedTimeMillis)), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            // Time Selector
            OutlinedCard(
                onClick = { showTimePicker = true },
                modifier = Modifier.weight(1f).height(72.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    Text("Time", color = Color.Gray, fontSize = 12.sp)
                    Text(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(selectedTimeMillis)), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. BASIC INFO ---
        OutlinedTextField(
            value = title, onValueChange = { title = it }, label = { Text("Alarm Title") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description, onValueChange = { description = it }, label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. CUSTOM ANIMATED VOLUME SLIDER ---
        Text("Alarm Volume", color = Color.White)
        CustomAnimatedVolumeSlider(volume = volume, onVolumeChange = { volume = it })

        Spacer(modifier = Modifier.height(16.dp))

        // --- 4. RECURRENCE ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
            Spacer(modifier = Modifier.width(8.dp))
            Text("Repeat Alarm", color = Color.White)
        }
        if (isRecurring) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("DAILY", "WEEKLY", "MONTHLY").forEach { type ->
                    FilterChip(
                        selected = recurrenceType == type, onClick = { recurrenceType = type }, label = { Text(type) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 5. AUDIO SOURCE ---
        Text("Audio Source", color = Color.White)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            FilterChip(selected = audioType == "TTS", onClick = { audioType = "TTS" }, label = { Text("Speech") })
            FilterChip(selected = audioType == "LOCAL_FILE", onClick = { audioType = "LOCAL_FILE" }, label = { Text("Local") })
            FilterChip(selected = audioType == "CLOUD_MEDIA", onClick = { audioType = "CLOUD_MEDIA" }, label = { Text("Cloud") })
        }

        when (audioType) {
            "TTS" -> OutlinedTextField(
                value = ttsContent, onValueChange = { ttsContent = it }, label = { Text("What should it say?") },
                modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            "LOCAL_FILE" -> Button(
                onClick = { audioPicker.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) { Text(if (localAudioUri == null) "Select Audio File" else "File Selected") }
            "CLOUD_MEDIA" -> OutlinedTextField(
                value = cloudUrl, onValueChange = { cloudUrl = it }, label = { Text("Stream URL") },
                modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SAVE BUTTON (Animated Spring) ---
        AnimatedPressButton(text = "Save & Schedule") {
            viewModel.createAlarm(
                title = title.ifBlank { "Health Alarm" },
                description = description,
                category = category,
                triggerTimeMillis = selectedTimeMillis,
                volumeLevel = volume,
                audioType = audioType,
                localUri = localAudioUri?.toString(),
                ttsContent = if (audioType == "TTS") ttsContent else null,
                cloudUrl = if (audioType == "CLOUD_MEDIA") cloudUrl else null,
                isRecurring = isRecurring,
                recurrenceType = if (isRecurring) recurrenceType else null,
                recurrenceInterval = if (isRecurring) 1 else null
            )
            onCreated()
        }
    }

    // Material 3 Dialogs
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = selectedTimeMillis
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                    }
                    selectedTimeMillis = cal.timeInMillis
                    showTimePicker = false
                }) { Text("OK") }
            },
            text = { TimePicker(state = timePickerState) } // Renders the gorgeous M3 Dial/Clock UI
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimeMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val calDate = Calendar.getInstance().apply { timeInMillis = millis }
                        val current = Calendar.getInstance().apply { timeInMillis = selectedTimeMillis }
                        current.set(Calendar.YEAR, calDate.get(Calendar.YEAR))
                        current.set(Calendar.MONTH, calDate.get(Calendar.MONTH))
                        current.set(Calendar.DAY_OF_MONTH, calDate.get(Calendar.DAY_OF_MONTH))
                        selectedTimeMillis = current.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

// --- CUSTOM ANIMATED VOLUME UI ---
@Composable
fun CustomAnimatedVolumeSlider(volume: Float, onVolumeChange: (Float) -> Unit) {
    val icon = when {
        volume == 0f -> Icons.Default.VolumeOff
        volume < 0.5f -> Icons.Default.VolumeDown
        else -> Icons.Default.VolumeUp
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Icon(icon, contentDescription = "Volume", tint = Color.LightGray)
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2C2C2C))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        val newVol = (change.position.x / size.width).coerceIn(0f, 1f)
                        onVolumeChange(newVol)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(volume)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text("${(volume * 100).toInt()}%", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(40.dp))
    }
}

// --- SPRING ANIMATED BUTTON ---
@Composable
fun AnimatedPressButton(text: String, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    Button(
        onClick = { onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.first().let {
                            isPressed = it.pressed
                        }
                    }
                }
            },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}