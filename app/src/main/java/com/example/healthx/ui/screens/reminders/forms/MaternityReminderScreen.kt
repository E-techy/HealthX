package com.example.healthx.ui.screens.reminders.forms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import com.example.healthx.ui.components.reminders.core.RepeatRuleBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaternityReminderScreen(onBack: () -> Unit) {
    var showAlarmConfig by remember { mutableStateOf(false) }
    var showRepeatConfig by remember { mutableStateOf(false) }

    // Form States
    var currentTrimester by remember { mutableStateOf("1st Trimester") }
    var pregnancyWeek by remember { mutableStateOf("") }
    var taskType by remember { mutableStateOf("") }

    var babyNotes by remember { mutableStateOf("") }
    var promptContractionTimer by remember { mutableStateOf(false) }

    val trimesters = listOf("1st Trimester", "2nd Trimester", "3rd Trimester")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Maternity Care", color = Color.White) },
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
                Text("Pregnancy Task & Tracking", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- SECTION 1: Timeline ---
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded, onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = currentTrimester, onValueChange = {}, readOnly = true,
                            label = { Text("Trimester") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded, onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color(0xFF2C2C2C))
                        ) {
                            trimesters.forEach { tri ->
                                DropdownMenuItem(
                                    text = { Text(tri, color = Color.White) },
                                    onClick = { currentTrimester = tri; expanded = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = pregnancyWeek, onValueChange = { pregnancyWeek = it },
                        label = { Text("Week (1-40)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- SECTION 2: Task Details ---
            item {
                OutlinedTextField(
                    value = taskType, onValueChange = { taskType = it },
                    label = { Text("Task (e.g., Fetal Kick Count, Prenatal Vit)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = babyNotes, onValueChange = { babyNotes = it },
                    label = { Text("Milestone Notes (e.g., Baby is size of a lemon)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- SECTION 3: Smart Tools ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Contraction Timer Prompt", color = Color.White)
                        Text("Adds a quick-start timer to this alarm if triggered.", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(checked = promptContractionTimer, onCheckedChange = { promptContractionTimer = it })
                }
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFF2C2C2C))
            }

            // --- SECTION 4: Expandable Accordions ---
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