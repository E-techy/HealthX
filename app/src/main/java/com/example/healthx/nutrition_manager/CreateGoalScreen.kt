package com.example.healthx.nutrition_manager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healthx.data.model.CreateGoalRequest
import com.example.healthx.data.model.GoalTarget
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalScreen(viewModel: NutritionViewModel) {
    // Form States
    var selectedGoalType by remember { mutableStateOf("MUSCLE_GAIN") }

    // Default Targets setup
    var caloriesTarget by remember { mutableStateOf("") }
    var proteinTarget by remember { mutableStateOf("") }
    var carbsTarget by remember { mutableStateOf("") }
    var fatTarget by remember { mutableStateOf("") }

    // Date Management
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    var startDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var endDateMillis by remember { mutableStateOf(System.currentTimeMillis() + 86400000L * 30) } // Default 30 days

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = NutritionUiTokens.DarkBackground,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(NutritionScreenState.Goals) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Initialize New Protocol", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        // Construct the targets array, filtering out empty ones
                        val targets = mutableListOf<GoalTarget>()
                        if (caloriesTarget.isNotBlank()) targets.add(GoalTarget("Calories", "$caloriesTarget kcal"))
                        if (proteinTarget.isNotBlank()) targets.add(GoalTarget("Protein", "${proteinTarget}g"))
                        if (carbsTarget.isNotBlank()) targets.add(GoalTarget("Carbs", "${carbsTarget}g"))
                        if (fatTarget.isNotBlank()) targets.add(GoalTarget("Fat", "${fatTarget}g"))

                        val request = CreateGoalRequest(
                            goalType = selectedGoalType,
                            targets = targets,
                            goalStartDate = isoFormatter.format(Date(startDateMillis)),
                            goalEndDate = isoFormatter.format(Date(endDateMillis))
                        )

                        viewModel.createNewGoal(request) {
                            viewModel.navigateTo(NutritionScreenState.Goals)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NutritionUiTokens.AccentColor)
                ) {
                    Text("Deploy Nutrition Goal", color = NutritionUiTokens.DarkBackground, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- GOAL TYPE SELECTION ---
            item {
                Text("Primary Objective", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                val goalTypes = listOf("MUSCLE_GAIN", "WEIGHT_LOSS", "MAINTENANCE", "BULK")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    goalTypes.forEach { type ->
                        val isSelected = selectedGoalType == type
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) NutritionUiTokens.AccentColor.copy(alpha = 0.1f) else NutritionUiTokens.SurfaceDark, RoundedCornerShape(12.dp))
                                .border(1.dp, if (isSelected) NutritionUiTokens.AccentColor else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { selectedGoalType = type }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedGoalType = type },
                                colors = RadioButtonDefaults.colors(selectedColor = NutritionUiTokens.AccentColor, unselectedColor = Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(type.replace("_", " "), color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- TIMELINE SELECTION ---
            item {
                Text("Operational Timeline", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Start Date Box
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Date", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedCard(
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.outlinedCardColors(containerColor = NutritionUiTokens.SurfaceDark),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = NutritionUiTokens.AccentColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dateFormatter.format(Date(startDateMillis)), color = Color.White)
                            }
                        }
                    }

                    // End Date Box
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End Date", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedCard(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.outlinedCardColors(containerColor = NutritionUiTokens.SurfaceDark),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = NutritionUiTokens.AccentColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dateFormatter.format(Date(endDateMillis)), color = Color.White)
                            }
                        }
                    }
                }
            }

            // --- MACRO TARGETS ---
            item {
                Text("Target Parameters", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Leave blank if not applicable to this protocol.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))

                MacroInputRow(label = "Daily Calories", suffix = "kcal", value = caloriesTarget, onValueChange = { caloriesTarget = it })
                MacroInputRow(label = "Protein", suffix = "grams", value = proteinTarget, onValueChange = { proteinTarget = it })
                MacroInputRow(label = "Carbohydrates", suffix = "grams", value = carbsTarget, onValueChange = { carbsTarget = it })
                MacroInputRow(label = "Fats", suffix = "grams", value = fatTarget, onValueChange = { fatTarget = it })

                Spacer(modifier = Modifier.height(40.dp)) // Buffer for bottom button
            }
        }
    }

    // --- DATE PICKER DIALOGS ---
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { startDateMillis = it }
                    showStartDatePicker = false
                }) { Text("Confirm", color = NutritionUiTokens.AccentColor) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDateMillis)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { endDateMillis = it }
                    showEndDatePicker = false
                }) { Text("Confirm", color = NutritionUiTokens.AccentColor) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun MacroInputRow(label: String, suffix: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = { Text(suffix, color = Color.Gray) },
            singleLine = true,
            modifier = Modifier.width(160.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = NutritionUiTokens.SurfaceDark,
                unfocusedContainerColor = NutritionUiTokens.SurfaceDark,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NutritionUiTokens.AccentColor,
                unfocusedBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}