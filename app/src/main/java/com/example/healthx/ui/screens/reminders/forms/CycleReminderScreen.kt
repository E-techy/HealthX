package com.example.healthx.ui.screens.reminders.forms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthx.ui.components.reminders.core.AlarmConfigBlock
import com.example.healthx.ui.components.reminders.core.RepeatRuleBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleReminderScreen(onBack: () -> Unit) {
    var showAlarmConfig by remember { mutableStateOf(false) }

    // Form States
    var selectedPhase by remember { mutableStateOf("Menstrual") }
    var expectedStartDate by remember { mutableStateOf("") }

    // Tracking Toggles
    var isFertilityWarning by remember { mutableStateOf(false) }
    var promptFlow by remember { mutableStateOf(true) }
    var promptMood by remember { mutableStateOf(true) }
    var promptPhysical by remember { mutableStateOf(true) }

    val phases = listOf("Menstrual", "Follicular", "Ovulation", "Luteal")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cycle & Tracking", color = Color.White) },
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
                Text("Setup Phase Log", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- SECTION 1: Phase Configuration ---
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedPhase,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Current/Target Phase") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded, onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF2C2C2C))
                    ) {
                        phases.forEach { phase ->
                            DropdownMenuItem(
                                text = { Text(phase, color = Color.White) },
                                onClick = { selectedPhase = phase; expanded = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // In a full app, this would be a DatePickerDialog
                OutlinedTextField(
                    value = expectedStartDate, onValueChange = { expectedStartDate = it },
                    label = { Text("Expected Phase Start Date (DD/MM/YYYY)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- SECTION 2: Logging Prompts ---
            item {
                Text("Daily Check-in Prompts", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Prompt to log Flow (Light/Medium/Heavy)", color = Color.White)
                    Switch(checked = promptFlow, onCheckedChange = { promptFlow = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Prompt to log Mood", color = Color.White)
                    Switch(checked = promptMood, onCheckedChange = { promptMood = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Prompt to log Symptoms (Cramps/Bloating)", color = Color.White)
                    Switch(checked = promptPhysical, onCheckedChange = { promptPhysical = it })
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fertility Window Warning", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Switch(checked = isFertilityWarning, onCheckedChange = { isFertilityWarning = it })
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFF2C2C2C))
            }

            // --- SECTION 3: Expandable Alarm Accordion ---
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