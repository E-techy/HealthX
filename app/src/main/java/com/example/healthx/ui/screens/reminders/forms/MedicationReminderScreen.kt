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

// NOTE: Import your AlarmConfigBlock and RepeatRuleBlock here!
import com.example.healthx.ui.components.reminders.core.AlarmConfigBlock
import com.example.healthx.ui.components.reminders.core.RepeatRuleBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationReminderScreen(onBack: () -> Unit) {
    // Accordion States to keep UI clean
    var showAlarmConfig by remember { mutableStateOf(false) }
    var showRepeatConfig by remember { mutableStateOf(false) }

    // Form States
    var medicineName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medications", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, tint = Color.White, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color.Black,
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: Save to Room/MongoDB */ }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Save, contentDescription = "Save")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // --- SECTION 1: Active Reminders List ---
            item {
                Text("Active Medications", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(2) { // Dummy list of existing reminders
                ExistingReminderListItem()
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = Color(0xFF2C2C2C))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Add New Medication", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- SECTION 2: The Clean Add Form ---
            item {
                OutlinedTextField(
                    value = medicineName, onValueChange = { medicineName = it },
                    label = { Text("Medicine Name") }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = dosage, onValueChange = { dosage = it },
                    label = { Text("Dosage (e.g., 1 Pill, 10ml)") }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- SECTION 3: Expandable Accordions ---
            item {
                // Repeat Schedule Accordion Header
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

                // The actual block expands smoothly here
                AnimatedVisibility(visible = showRepeatConfig) {
                    RepeatRuleBlock(onRuleChanged = { /* Save to local state */ })
                }
                HorizontalDivider(color = Color(0xFF2C2C2C))
            }

            item {
                // Alarm Config Accordion Header
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

                // The actual block expands smoothly here
                AnimatedVisibility(visible = showAlarmConfig) {
                    AlarmConfigBlock(onConfigChanged = { /* Save to local state */ })
                }
                HorizontalDivider(color = Color(0xFF2C2C2C))

                Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB
            }
        }
    }
}

// --- The 3-Dot Menu List Item ---
@Composable
fun ExistingReminderListItem() {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Vitamin D3", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Daily • 08:00 AM", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color(0xFF2C2C2C))
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Reminder", color = Color.White) },
                        onClick = { expanded = false },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White) }
                    )
                    DropdownMenuItem(
                        text = { Text("Sync to Cloud", color = Color.White) },
                        onClick = { expanded = false },
                        leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White) }
                    )
                    HorizontalDivider(color = Color.DarkGray)
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        onClick = { expanded = false },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                    )
                }
            }
        }
    }
}