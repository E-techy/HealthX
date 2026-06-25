package com.example.healthx.ui.components.reminders.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healthx.data.models.reminders.core.RepeatRule
import com.example.healthx.data.models.reminders.core.RepeatType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeatRuleBlock(
    initialRule: RepeatRule = RepeatRule(repeatType = RepeatType.ONCE),
    onRuleChanged: (RepeatRule) -> Unit
) {
    var repeatType by remember { mutableStateOf(initialRule.repeatType) }
    var selectedDays by remember { mutableStateOf(initialRule.specificDays ?: emptyList()) }
    var intervalStep by remember { mutableStateOf(initialRule.intervalStep?.toString() ?: "") }

    fun updateParent() {
        onRuleChanged(
            RepeatRule(
                repeatType = repeatType,
                specificDays = if (repeatType == RepeatType.SPECIFIC_DAYS) selectedDays else null,
                intervalStep = if (repeatType == RepeatType.RANGE_WITH_STEP) intervalStep.toIntOrNull() else null
            )
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Repeat Schedule", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))

            // Dropdown for Repeat Type
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = repeatType.name.replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color(0xFF2C2C2C))
                ) {
                    RepeatType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name.replace("_", " "), color = Color.White) },
                            onClick = {
                                repeatType = type
                                expanded = false
                                updateParent()
                            }
                        )
                    }
                }
            }

            // Dynamic Sub-UI based on selection
            if (repeatType == RepeatType.SPECIFIC_DAYS) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Days (1=Mon, 7=Sun)", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    items((1..7).toList()) { day ->
                        val isSelected = selectedDays.contains(day)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                                updateParent()
                            },
                            label = { Text("D$day") }
                        )
                    }
                }
            }

            if (repeatType == RepeatType.RANGE_WITH_STEP) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = intervalStep,
                    onValueChange = {
                        intervalStep = it
                        updateParent()
                    },
                    label = { Text("Gap (e.g., Every 2 days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        }
    }
}