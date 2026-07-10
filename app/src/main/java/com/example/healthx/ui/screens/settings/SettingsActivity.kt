package com.example.healthx.ui.screens.settings

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

    // Form inputs matching state parameters
    var nameState by remember { mutableStateOf("") }
    var weightState by remember { mutableStateOf("") }
    var heightState by remember { mutableStateOf("") }
    var ethnicityState by remember { mutableStateOf("") }
    var countryState by remember { mutableStateOf("") }
    var customCountryState by remember { mutableStateOf("") }
    var stateRegionState by remember { mutableStateOf("") }
    var languageState by remember { mutableStateOf("") }

    // Display mode control configurations
    var isEditingProfile by remember { mutableStateOf(false) }

    // Dropdown visual toggle selectors
    var ethnicityExpanded by remember { mutableStateOf(false) }
    var countryExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }

    // Sub-elements text variables
    var newAllergyText by remember { mutableStateOf("") }
    var selectedCompany by remember { mutableStateOf("Google") }
    var selectedModel by remember { mutableStateOf("gemini-2.5-flash") }
    var apiKeyValueState by remember { mutableStateOf("") }
    var companyExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }

    // Sync database data payload cleanly without dropping localized input states
    LaunchedEffect(settingsData) {
        nameState = settingsData.name ?: ""
        weightState = settingsData.weight ?: ""
        heightState = settingsData.height ?: ""
        ethnicityState = settingsData.ethnicity ?: ""

        val incomingCountry = settingsData.country ?: ""
        if (viewModel.countryOptions.contains(incomingCountry) || incomingCountry.isBlank()) {
            countryState = incomingCountry
            customCountryState = ""
        } else {
            countryState = "Other"
            customCountryState = incomingCountry
        }

        stateRegionState = settingsData.state ?: ""
        languageState = settingsData.preferredLanguage ?: ""
    }

    LaunchedEffect(Unit) {
        viewModel.fetchSettings()
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(uiState) {
        when (uiState) {
            is SettingsUiState.Success -> {
                Toast.makeText(context, (uiState as SettingsUiState.Success).message, Toast.LENGTH_SHORT).show()
                isEditingProfile = false
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
                title = { Text("Profile Dashboard", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchSettings() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh remote configuration")
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
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Premium Profile Visual Header Card Component
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {                                Box(
                                    modifier = Modifier.size(54.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
                                }
                                Column {
                                    Text(
                                        text = nameState.ifBlank { "Anonymous User" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Metrics Summary Tracker",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (isEditingProfile) {
                                        // Reset configurations back to original when canceling out
                                        viewModel.fetchSettings()
                                    }
                                    isEditingProfile = !isEditingProfile
                                },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Icon(
                                    imageVector = if (isEditingProfile) Icons.Default.Close else Icons.Default.Edit,
                                    contentDescription = "Toggle editing state",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Physical metrics block rendering configuration layout
                        if (!isEditingProfile) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Weight", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(weightState.ifBlank { "Not Set" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                }
                                Column {
                                    Text("Height", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(heightState.ifBlank { "Not Set" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                }
                                Column(modifier = Modifier.padding(end = 16.dp)) {
                                    Text("Language", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(languageState.ifBlank { "Not Set" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(value = nameState, onValueChange = { nameState = it }, label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(value = weightState, onValueChange = { weightState = it }, label = { Text("Weight (e.g. 70kg)") }, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = heightState, onValueChange = { heightState = it }, label = { Text("Height (e.g. 175cm)") }, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Advanced Regional Custom Layout Card Section
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                        // Advanced Ethnicity Dropdown Implementation
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = ethnicityState,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Ethnicity") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { ethnicityExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenu(expanded = ethnicityExpanded, onDismissRequest = { ethnicityExpanded = false }, modifier = Modifier.fillMaxWidth(0.85f)) {
                                viewModel.ethnicityOptions.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = {
                                        ethnicityState = option
                                        ethnicityExpanded = false
                                    })
                                }
                            }
                        }

                        // Advanced Cascading Country Dropdown Setup
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = countryState,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Country") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { countryExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenu(expanded = countryExpanded, onDismissRequest = { countryExpanded = false }, modifier = Modifier.fillMaxWidth(0.85f)) {
                                viewModel.countryOptions.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = {
                                        countryState = option
                                        countryExpanded = false
                                    })
                                }
                            }
                        }

                        // Conditional Visibility rendering logic when target country selection matches "Other"
                        AnimatedVisibility(visible = countryState == "Other") {
                            OutlinedTextField(
                                value = customCountryState,
                                onValueChange = { customCountryState = it },
                                label = { Text("Specify Country Name") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                            )
                        }

                        OutlinedTextField(value = stateRegionState, onValueChange = { stateRegionState = it }, label = { Text("State / Territory") }, modifier = Modifier.fillMaxWidth())

                        // Advanced Preferred Language Dropdown Implementation
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = languageState,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Preferred Language") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { languageExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenu(expanded = languageExpanded, onDismissRequest = { languageExpanded = false }, modifier = Modifier.fillMaxWidth(0.85f)) {
                                viewModel.languageOptions.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = {
                                        languageState = option
                                        languageExpanded = false
                                    })
                                }
                            }
                        }
                    }
                }

                // Global Preferences Layout (Theme Configuration Management)
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        OutlinedTextField(
                            value = settingsData.theme.uppercase(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Visual Theme Mode") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                TextButton(onClick = { themeExpanded = true }) { Text("Adjust") }
                            }
                        )
                        DropdownMenu(expanded = themeExpanded, onDismissRequest = { themeExpanded = false }) {
                            listOf("system", "light", "dark").forEach { mode ->
                                DropdownMenuItem(text = { Text(mode.uppercase()) }, onClick = {
                                    viewModel.updateSettings(settingsData.copy(theme = mode))
                                    themeExpanded = false
                                })
                            }
                        }
                    }
                }

                // Medical/Allergies Management Engine UI Cards Section
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Allergies Profiles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newAllergyText,
                                onValueChange = { newAllergyText = it },
                                label = { Text("Add Allergy") },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                if (newAllergyText.isNotBlank()) {
                                    viewModel.addAllergy(newAllergyText)
                                    newAllergyText = ""
                                }
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Insert")
                            }
                        }

                        // Modern Chip list presentation mapping
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            settingsData.allergies.forEach { allergy ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp)).padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(allergy, style = MaterialTheme.typography.bodyMedium)
                                    Icon(Icons.Default.Close, contentDescription = "Drop", modifier = Modifier.size(18.dp).clickable {
                                        viewModel.removeAllergy(allergy)
                                    })
                                }
                            }
                        }
                    }
                }

                // AI Engine Token Authorization Configurations Layout Card Component
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("AI Key Integrations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        // Providers selection drop-down matrix component
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(value = selectedCompany, onValueChange = {}, readOnly = true, label = { Text("Provider Engine") }, modifier = Modifier.fillMaxWidth(), trailingIcon = {
                                IconButton(onClick = { companyExpanded = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                            })
                            DropdownMenu(expanded = companyExpanded, onDismissRequest = { companyExpanded = false }) {
                                viewModel.aiProviders.forEach { company ->
                                    DropdownMenuItem(text = { Text(company) }, onClick = {
                                        selectedCompany = company
                                        selectedModel = viewModel.modelsMap[company]?.first() ?: ""
                                        companyExpanded = false
                                    })
                                }
                            }
                        }

                        // Dependent model execution specs cascading mapping
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(value = selectedModel, onValueChange = {}, readOnly = true, label = { Text("Model Profile Definition") }, modifier = Modifier.fillMaxWidth(), trailingIcon = {
                                IconButton(onClick = { modelExpanded = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                            })
                            DropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                                (viewModel.modelsMap[selectedCompany] ?: emptyList()).forEach { mappingModel ->
                                    DropdownMenuItem(text = { Text(mappingModel) }, onClick = {
                                        selectedModel = mappingModel
                                        modelExpanded = false
                                    })
                                }
                            }
                        }

                        OutlinedTextField(value = apiKeyValueState, onValueChange = { apiKeyValueState = it }, label = { Text("Secure Access Token Key") }, modifier = Modifier.fillMaxWidth())

                        Button(
                            onClick = {
                                if (apiKeyValueState.isNotBlank()) {
                                    viewModel.addApiKey(ApiKeyItem(selectedCompany, selectedModel, apiKeyValueState))
                                    apiKeyValueState = ""
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Map Key Infrastructure")
                        }

                        // Active security configuration entries linked to active profiles array mapping
                        settingsData.apiKeys.forEach { integrationKey ->
                            Row(
                                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp)).padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${integrationKey.companyName} » ${integrationKey.modelName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("•••• •••• " + integrationKey.apiKeyValue.takeLast(4), style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = { viewModel.removeApiKey(integrationKey) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Purge Configuration Item", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // Persistent Execution Deployment Save Actions Button Component
                Button(
                    onClick = {
                        val validatedCountryPayload = if (countryState == "Other") customCountryState.trim() else countryState
                        val combinedSubmissionData = settingsData.copy(
                            name = nameState.ifBlank { null },
                            weight = weightState.ifBlank { null },
                            height = heightState.ifBlank { null },
                            ethnicity = ethnicityState.ifBlank { null },
                            country = validatedCountryPayload.ifBlank { null },
                            state = stateRegionState.ifBlank { null },
                            preferredLanguage = languageState.ifBlank { null }
                        )
                        viewModel.updateSettings(combinedSubmissionData)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply & Sync Server Profile", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}