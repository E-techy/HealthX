package com.example.healthx.ui.screens.settings

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthx.data.network.ApiKeyItem
import com.example.healthx.ui.theme.HealthXTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(onBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(), onBack: () -> Unit) {
    val settingsData by viewModel.settingsData.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Local input view states
    var nameState by remember { mutableStateOf("") }
    var weightState by remember { mutableStateOf("") }
    var heightState by remember { mutableStateOf("") }
    var ethnicityState by remember { mutableStateOf("") }
    var countryState by remember { mutableStateOf("") }
    var stateRegionState by remember { mutableStateOf("") }
    var languageState by remember { mutableStateOf("") }

    var themeExpanded by remember { mutableStateOf(false) }
    var newAllergyText by remember { mutableStateOf("") }

    // API Key temporary selection mechanics
    var selectedCompany by remember { mutableStateOf("Google") }
    var selectedModel by remember { mutableStateOf("gemini-2.5-flash") }
    var apiKeyValueState by remember { mutableStateOf("") }
    var companyExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }

    // Sync database data payload to view state when fetched
    LaunchedEffect(settingsData) {
        nameState = settingsData.name ?: ""
        weightState = settingsData.weight ?: ""
        heightState = settingsData.height ?: ""
        ethnicityState = settingsData.ethnicity ?: ""
        countryState = settingsData.country ?: ""
        stateRegionState = settingsData.state ?: ""
        languageState = settingsData.preferredLanguage ?: ""
    }

    // Trigger Initial Data Fetch
    LaunchedEffect(Unit) {
        viewModel.fetchSettings()
    }

    // Handle incoming state notifications
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(uiState) {
        when (uiState) {
            is SettingsUiState.Success -> {
                Toast.makeText(context, (uiState as SettingsUiState.Success).message, Toast.LENGTH_SHORT).show()
                viewModel.resetUiState()
            }
            is SettingsUiState.Error -> {
                Toast.makeText(context, (uiState as SettingsUiState.Error).exception, Toast.LENGTH_LONG).show()
                viewModel.resetUiState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchSettings() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState is SettingsUiState.Loading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile & Bio Section
                Text("User Profile Info", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = nameState, onValueChange = { nameState = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = weightState, onValueChange = { weightState = it }, label = { Text("Weight") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = heightState, onValueChange = { heightState = it }, label = { Text("Height") }, modifier = Modifier.fillMaxWidth())

                Divider()

                // Demographics
                Text("Demographics & Locale", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = ethnicityState, onValueChange = { ethnicityState = it }, label = { Text("Ethnicity") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = countryState, onValueChange = { countryState = it }, label = { Text("Country") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = stateRegionState, onValueChange = { stateRegionState = it }, label = { Text("State") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = languageState, onValueChange = { languageState = it }, label = { Text("Preferred Language") }, modifier = Modifier.fillMaxWidth())

                Divider()

                // Theme Dropdown Selector
                Text("App Preferences", style = MaterialTheme.typography.titleMedium)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = settingsData.theme.uppercase(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Theme Preference") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(onClick = { themeExpanded = true }) { Text("Select") }
                        }
                    )
                    DropdownMenu(expanded = themeExpanded, onDismissRequest = { themeExpanded = false }) {
                        listOf("system", "light", "dark").forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection.uppercase()) },
                                onClick = {
                                    viewModel.updateSettings(settingsData.copy(theme = selection))
                                    themeExpanded = false
                                }
                            )
                        }
                    }
                }

                Divider()

                // Allergies Management Engine
                Text("Allergies List Management", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newAllergyText,
                        onValueChange = { newAllergyText = it },
                        label = { Text("Add New Allergy") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (newAllergyText.isNotBlank()) {
                            viewModel.addAllergy(newAllergyText)
                            newAllergyText = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item")
                    }
                }

                // Render lists of strings inside flow container row mappings
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    settingsData.allergies.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)).padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item)
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.clickable {
                                viewModel.removeAllergy(item)
                            })
                        }
                    }
                }

                Divider()

                // Dynamic Cascading AI Token Integration Context
                Text("External AI Model API Keys Integration", style = MaterialTheme.typography.titleMedium)

                // Company Dropdown Selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = selectedCompany, onValueChange = {}, readOnly = true, label = { Text("AI Provider") }, modifier = Modifier.fillMaxWidth(), trailingIcon = {
                        TextButton(onClick = { companyExpanded = true }) { Text("Change") }
                    })
                    DropdownMenu(expanded = companyExpanded, onDismissRequest = { companyExpanded = false }) {
                        viewModel.aiProviders.forEach { item ->
                            DropdownMenuItem(text = { Text(item) }, onClick = {
                                selectedCompany = item
                                selectedModel = viewModel.modelsMap[item]?.first() ?: ""
                                companyExpanded = false
                            })
                        }
                    }
                }

                // Model Dropdown Selection (Dependent on Company)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = selectedModel, onValueChange = {}, readOnly = true, label = { Text("Model Spec") }, modifier = Modifier.fillMaxWidth(), trailingIcon = {
                        TextButton(onClick = { modelExpanded = true }) { Text("Change") }
                    })
                    DropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                        (viewModel.modelsMap[selectedCompany] ?: emptyList()).forEach { modelOpt ->
                            DropdownMenuItem(text = { Text(modelOpt) }, onClick = {
                                selectedModel = modelOpt
                                modelExpanded = false
                            })
                        }
                    }
                }

                OutlinedTextField(value = apiKeyValueState, onValueChange = { apiKeyValueState = it }, label = { Text("API Key Token Value") }, modifier = Modifier.fillMaxWidth())

                Button(
                    onClick = {
                        if (apiKeyValueState.isNotBlank()) {
                            viewModel.addApiKey(ApiKeyItem(selectedCompany, selectedModel, apiKeyValueState))
                            apiKeyValueState = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Add/Update Configuration Key")
                }

                // Active Model Infrastructure Configurations Linked to This Account
                settingsData.apiKeys.forEach { key ->
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp)).padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${key.companyName} (${key.modelName})", style = MaterialTheme.typography.bodyMedium)
                            Text("••••••••" + key.apiKeyValue.takeLast(4), style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { viewModel.removeApiKey(key) }) {
                            Icon(Icons.Default.Close, contentDescription = "Delete configuration key reference")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Global Save Updates trigger button
                Button(
                    onClick = {
                        val payload = settingsData.copy(
                            name = nameState.ifBlank { null },
                            weight = weightState.ifBlank { null },
                            height = heightState.ifBlank { null },
                            ethnicity = ethnicityState.ifBlank { null },
                            country = countryState.ifBlank { null },
                            state = stateRegionState.ifBlank { null },
                            preferredLanguage = languageState.ifBlank { null }
                        )
                        viewModel.updateSettings(payload)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Save Configuration Profile")
                }
            }
        }
    }
}