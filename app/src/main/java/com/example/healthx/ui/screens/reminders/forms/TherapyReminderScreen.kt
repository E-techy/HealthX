package com.example.healthx.ui.screens.reminders.forms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthx.ui.components.reminders.core.AlarmConfigBlock
import com.example.healthx.ui.components.reminders.core.RepeatRuleBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TherapyReminderScreen(onBack: () -> Unit) {
    var showAlarmConfig by remember { mutableStateOf(false) }
    var showRepeatConfig by remember { mutableStateOf(false) }

    // Form States
    var therapistName by remember { mutableStateOf("") }
    var sessionType by remember { mutableStateOf("") }
    var homeworkNotes by remember { mutableStateOf("") }

    // Logistics
    var isTelehealth by remember { mutableStateOf(true) }
    var linkOrLocation by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Therapy Session", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, tint = Color.White, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color.Black,
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Save to DB */ }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Save, contentDescription = "Save")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text("Schedule Therapy", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- SECTION 1: Session Details ---
            item {
                OutlinedTextField(
                    value = therapistName, onValueChange = { therapistName = it },
                    label = { Text("Therapist / Counselor Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = sessionType, onValueChange = { sessionType = it },
                    label = { Text("Session Type (e.g., CBT, Physiotherapy)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- SECTION 2: Logistics & Prep ---
            item {
                Text("Meeting Logistics", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Is this an online meeting?", color = Color.White)
                    Switch(checked = isTelehealth, onCheckedChange = { isTelehealth = it })
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = linkOrLocation, onValueChange = { linkOrLocation = it },
                    label = { Text(if (isTelehealth) "Zoom/Meet Link" else "Clinic Address") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = homeworkNotes, onValueChange = { homeworkNotes = it },
                    label = { Text("Pre-Session Homework / Topics to Discuss") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFF2C2C2C))
            }

            // --- SECTION 3: Expandable Accordions ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showRepeatConfig = !showRepeatConfig }.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Repeat, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Repeat Schedule", color = Color.White, fontSize = 16.sp)
                    }
                    Icon(if (showRepeatConfig) Icons.Default.ExpandLess else Icons.Default.ExpandMore, tint = Color.Gray, contentDescription = null)
                }
                AnimatedVisibility(visible = showRepeatConfig) { RepeatRuleBlock(onRuleChanged = { }) }
                HorizontalDivider(color = Color(0xFF2C2C2C))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showAlarmConfig = !showAlarmConfig }.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Alarm & Audio Settings", color = Color.White, fontSize = 16.sp)
                    }
                    Icon(if (showAlarmConfig) Icons.Default.ExpandLess else Icons.Default.ExpandMore, tint = Color.Gray, contentDescription = null)
                }
                AnimatedVisibility(visible = showAlarmConfig) { AlarmConfigBlock(onConfigChanged = { }) }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}