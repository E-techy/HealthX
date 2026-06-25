package com.example.healthx.ui.screens.reminders.forms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthx.ui.components.reminders.core.AlarmConfigBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationReminderScreen(onBack: () -> Unit) {
    var showAlarmConfig by remember { mutableStateOf(false) }

    // Form States
    var vaccineName by remember { mutableStateOf("") }
    var targetDisease by remember { mutableStateOf("") }
    var doseNumber by remember { mutableStateOf("1") }
    var totalDoses by remember { mutableStateOf("1") }
    var clinicName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Immunization", color = Color.White) },
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
                Text("Schedule Vaccination", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- SECTION 1: Vaccine Details ---
            item {
                OutlinedTextField(
                    value = vaccineName, onValueChange = { vaccineName = it },
                    label = { Text("Vaccine Name (e.g., Covishield, Tdap)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetDisease, onValueChange = { targetDisease = it },
                    label = { Text("Target Disease (e.g., COVID-19, Tetanus)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- SECTION 2: Dosing Schedule ---
            item {
                Text("Dose Information", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = doseNumber, onValueChange = { doseNumber = it },
                        label = { Text("Dose #") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = totalDoses, onValueChange = { totalDoses = it },
                        label = { Text("Total Doses Required") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- SECTION 3: Logistics & Files ---
            item {
                OutlinedTextField(
                    value = clinicName, onValueChange = { clinicName = it },
                    label = { Text("Clinic or Hospital Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Placeholder for document attachment
                OutlinedButton(onClick = { /* Trigger file picker */ }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Attach Certificate or Receipt")
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFF2C2C2C))
            }

            // --- SECTION 4: Expandable Alarm Accordion ---
            // Note: We skip the RepeatRule block here because vaccines are generally scheduled as specific, individual dates rather than a recurring cycle.
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