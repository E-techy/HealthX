package com.example.healthx.nutrition_manager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healthx.data.model.CreateGoalRequest
import com.example.healthx.data.model.GoalTarget
import java.text.SimpleDateFormat
import java.util.*

// --- DICTIONARY FOR ADVANCED NUTRIENT SELECTION ---
object NutrientDictionary {
    val categories = mapOf(
        "Energy & Main Macros" to listOf("Calories", "Protein", "Carbs", "Fat", "Fiber", "Sugar", "Added Sugar", "Saturated Fat", "Trans Fat", "Cholesterol", "Sodium", "Water Volume"),
        "Secondary Fats & Carbs" to listOf("Starch", "Monounsaturated Fat", "Polyunsaturated Fat", "Omega 3", "Omega 6"),
        "Minerals" to listOf("Calcium", "Iron", "Magnesium", "Phosphorus", "Potassium", "Zinc", "Copper", "Manganese", "Selenium", "Iodine", "Chloride", "Chromium", "Molybdenum"),
        "Vitamins" to listOf("Vitamin A", "Vitamin B1", "Vitamin B2", "Vitamin B3", "Vitamin B5", "Vitamin B6", "Vitamin B7", "Vitamin B9", "Vitamin B12", "Vitamin C", "Vitamin D", "Vitamin E", "Vitamin K", "Vitamin D2", "Vitamin D3"),
        "Amino Acids" to listOf("Tryptophan", "Threonine", "Isoleucine", "Leucine", "Lysine", "Methionine", "Cystine", "Phenylalanine", "Tyrosine", "Valine", "Arginine", "Histidine", "Alanine", "Aspartic Acid", "Glutamic Acid", "Glycine", "Proline", "Serine"),
        "Other Compounds" to listOf("Caffeine", "Taurine", "Alcohol", "Ash", "Choline", "Fluoride", "Lutein", "Zeaxanthin", "Lycopene", "Beta Carotene", "Alpha Carotene", "Beta Cryptoxanthin", "Retinol")
    )

    val units = listOf("g", "mg", "mcg", "kcal", "ml", "L", "oz", "kg", "IU")
}

// Custom State Class for dynamic rows
class TargetState(initialName: String, initialUnit: String) {
    var name by mutableStateOf(initialName)
    var amount by mutableStateOf("")
    var unit by mutableStateOf(initialUnit)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalScreen(viewModel: NutritionViewModel) {
    // Form States
    var selectedGoalType by remember { mutableStateOf("MUSCLE_GAIN") }

    // Dynamic Targets List (Defaults to just Calories and Protein to start)
    val targetItems = remember {
        mutableStateListOf(
            TargetState("Calories", "kcal"),
            TargetState("Protein", "g")
        )
    }

    // Dialog States
    var showNutrientDialog by remember { mutableStateOf(false) }

    // Date Management
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", LocalLocale.current.platformLocale)
    val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", LocalLocale.current.platformLocale).apply {
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
                        // Construct the targets array, filtering out empty ones (Solves the "0" issue)
                        val targets = targetItems
                            .filter { it.amount.isNotBlank() && it.amount.toFloatOrNull() != null && it.amount.toFloat() > 0 }
                            .map { GoalTarget(it.name, "${it.amount} ${it.unit}") }

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

            // --- DYNAMIC MACRO & NUTRIENT TARGETS ---
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Target Parameters", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Track any nutrient, vitamin, or metric.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { showNutrientDialog = true }, modifier = Modifier.background(NutritionUiTokens.AccentColor.copy(alpha = 0.2f), CircleShape)) {
                        Icon(Icons.Default.Add, contentDescription = "Add Nutrient", tint = NutritionUiTokens.AccentColor)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(targetItems) { targetState ->
                DynamicTargetRow(
                    targetState = targetState,
                    onDelete = { targetItems.remove(targetState) }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) } // Buffer for bottom button
        }
    }

    // --- DIALOGS ---
    if (showNutrientDialog) {
        NutrientSelectionDialog(
            onDismiss = { showNutrientDialog = false },
            onNutrientSelected = { nutrientName ->
                // Determine a smart default unit based on the nutrient name
                val defaultUnit = when {
                    nutrientName.contains("Calories", true) -> "kcal"
                    nutrientName.contains("Water", true) -> "ml"
                    nutrientName.contains("Vitamin", true) || nutrientName.contains("Omega", true) -> "mg"
                    else -> "g"
                }
                targetItems.add(TargetState(nutrientName, defaultUnit))
                showNutrientDialog = false
            }
        )
    }

    // Date Pickers (Same as before)
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
        ) { DatePicker(state = datePickerState) }
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
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
fun DynamicTargetRow(targetState: TargetState, onDelete: () -> Unit) {
    var expandedUnit by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).background(NutritionUiTokens.SurfaceDark, RoundedCornerShape(12.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Delete Button
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.7f))
        }

        // Nutrient Name
        Text(targetState.name, color = Color.White, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)

        // Value Input
        OutlinedTextField(
            value = targetState.amount,
            onValueChange = { targetState.amount = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text("0", color = Color.DarkGray) },
            singleLine = true,
            modifier = Modifier.width(90.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = NutritionUiTokens.DarkBackground,
                unfocusedContainerColor = NutritionUiTokens.DarkBackground,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NutritionUiTokens.AccentColor,
                unfocusedBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Unit Dropdown
        Box {
            Text(
                text = targetState.unit,
                color = NutritionUiTokens.AccentColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { expandedUnit = true }
                    .background(NutritionUiTokens.DarkBackground, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            )
            DropdownMenu(
                expanded = expandedUnit,
                onDismissRequest = { expandedUnit = false },
                modifier = Modifier.background(NutritionUiTokens.SurfaceDark)
            ) {
                NutrientDictionary.units.forEach { unitSelection ->
                    DropdownMenuItem(
                        text = { Text(unitSelection, color = Color.White) },
                        onClick = {
                            targetState.unit = unitSelection
                            expandedUnit = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutrientSelectionDialog(onDismiss: () -> Unit, onNutrientSelected: (String) -> Unit) {
    var customNutrient by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NutritionUiTokens.SurfaceDark
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxHeight(0.85f)) {
            Text("Add Target Parameter", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // Custom Nutrient Input
            OutlinedTextField(
                value = customNutrient,
                onValueChange = { customNutrient = it },
                placeholder = { Text("Or type a custom metric (e.g. Sleep hrs)", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (customNutrient.isNotBlank()) {
                        IconButton(onClick = { onNutrientSelected(customNutrient) }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Custom", tint = NutritionUiTokens.AccentColor)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NutritionUiTokens.DarkBackground,
                    unfocusedContainerColor = NutritionUiTokens.DarkBackground,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NutritionUiTokens.AccentColor,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Categorized Dictionary List
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                NutrientDictionary.categories.forEach { (category, nutrients) ->
                    item {
                        Text(
                            text = category,
                            color = NutritionUiTokens.AccentColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        )
                    }
                    items(nutrients) { nutrient ->
                        Text(
                            text = nutrient,
                            color = Color.LightGray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNutrientSelected(nutrient) }
                                .padding(vertical = 12.dp, horizontal = 16.dp)
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}