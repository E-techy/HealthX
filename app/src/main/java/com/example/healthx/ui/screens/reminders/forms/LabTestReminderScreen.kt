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
fun LabTestReminderScreen(onBack: () -> Unit) {
    var showAlarmConfig by remember { mutableStateOf(false) }

    // Form States
    var testName by remember { mutableStateOf("") }
    var prepNotes by remember { mutableStateOf("") }

    // Logistics
    var isHomeCollection by remember { mutableStateOf(false) }
    var locationDetails by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lab Diagnostics", color = Color.White) },
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
                Text("Schedule a Lab Test", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- SECTION 1: Test Details ---
            item {
                OutlinedTextField(
                    value = testName, onValueChange = { testName = it },
                    label = { Text("Test Name (e.g., CBC, Lipid Profile)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = prepNotes, onValueChange = { prepNotes = it },
                    label = { Text("Preparation Notes") },
                    placeholder = { Text("e.g., 10 hour fasting, drink 2L water") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- SECTION 2: Logistics & Location ---
            item {
                Text("Collection Details", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Home Sample Collection", color = Color.White)
                    Switch(checked = isHomeCollection, onCheckedChange = { isHomeCollection = it })
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = locationDetails, onValueChange = { locationDetails = it },
                    label = { Text(if (isHomeCollection) "Your Home Address" else "Lab / Hospital Address") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Button placeholder for uploading the Doctor's written prescription for the lab test
                OutlinedButton(onClick = { /* Trigger file picker */ }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Attach Doctor's Prescription")
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