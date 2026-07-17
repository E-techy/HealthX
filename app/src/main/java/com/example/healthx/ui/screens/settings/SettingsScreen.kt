package com.example.healthx.ui.screens.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthx.data.local.SessionManager
import com.example.healthx.data.network.ApiKeyItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(), onBack: () -> Unit) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val delegatedSession by sessionManager.delegatedSessionFlow.collectAsState()

    // --- PERMISSION ENGINE ---
    val isGuest = delegatedSession != null
    val canEditSettings = !isGuest || delegatedSession!!.hasPermission("EDIT_SETTINGS")

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
        android.util.Log.d("SettingsScreen", "🚀 Screen Loaded. isGuest: $isGuest, canEdit: $canEditSettings")
        viewModel.fetchSettings()
    }

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

    // --- DARK MODE TOKENS ---
    val bgColor = Color(0xFF0A0A12)
    val cardColor = Color(0xFF1A1A2E)
    val accentColor = Color(0xFF1E88E5)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = { Text("Profile Dashboard", fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchSettings() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh remote configuration", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        }
    ) { innerPadding ->
        if (uiState is SettingsUiState.Loading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentColor)
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

                // READ-ONLY BANNER
                if (isGuest && !canEditSettings) {
                    Surface(
                        color = Color(0xFFE65100).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFFB74D))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Read-Only Mode Active", color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("You do not have permission to edit ${delegatedSession?.name}'s settings.", color = Color.LightGray, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // --- 1. CORE PROFILE METRICS ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier.size(54.dp).clip(CircleShape).background(accentColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                                Column {
                                    Text(nameState.ifBlank { "Anonymous User" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Metrics Summary Tracker", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }

                            // HIDE EDIT TOGGLE IF NO PERMISSION
                            if (canEditSettings) {
                                IconButton(
                                    onClick = {
                                        if (isEditingProfile) viewModel.fetchSettings() // Reset on cancel
                                        isEditingProfile = !isEditingProfile
                                    },
                                    modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                                ) {
                                    Icon(if (isEditingProfile) Icons.Default.Close else Icons.Default.Edit, contentDescription = "Toggle edit", tint = accentColor)
                                }
                            }
                        }

                        if (!isEditingProfile) {
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Weight", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(weightState.ifBlank { "Not Set" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = Color.White)
                                }
                                Column {
                                    Text("Height", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(heightState.ifBlank { "Not Set" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = Color.White)
                                }
                                Column(modifier = Modifier.padding(end = 16.dp)) {
                                    Text("Language", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(languageState.ifBlank { "Not Set" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = Color.White)
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                CustomTextField(value = nameState, onValueChange = { nameState = it }, label = "Display Name")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    CustomTextField(value = weightState, onValueChange = { weightState = it }, label = "Weight (e.g. 70kg)", modifier = Modifier.weight(1f))
                                    CustomTextField(value = heightState, onValueChange = { heightState = it }, label = "Height (e.g. 175cm)", modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // --- 2. DEMOGRAPHICS & REGION ---
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = cardColor)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                        // Ethnicity
                        Box(modifier = Modifier.fillMaxWidth()) {
                            CustomTextField(
                                value = ethnicityState, onValueChange = {}, readOnly = true, label = "Ethnicity",
                                trailingIcon = {
                                    if (canEditSettings) IconButton(onClick = { ethnicityExpanded = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray) }
                                }
                            )
                            DropdownMenu(expanded = ethnicityExpanded, onDismissRequest = { ethnicityExpanded = false }, modifier = Modifier.background(cardColor)) {
                                viewModel.ethnicityOptions.forEach { option ->
                                    DropdownMenuItem(text = { Text(option, color = Color.White) }, onClick = { ethnicityState = option; ethnicityExpanded = false })
                                }
                            }
                        }

                        // Country
                        Box(modifier = Modifier.fillMaxWidth()) {
                            CustomTextField(
                                value = countryState, onValueChange = {}, readOnly = true, label = "Country",
                                trailingIcon = {
                                    if (canEditSettings) IconButton(onClick = { countryExpanded = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray) }
                                }
                            )
                            DropdownMenu(expanded = countryExpanded, onDismissRequest = { countryExpanded = false }, modifier = Modifier.background(cardColor)) {
                                viewModel.countryOptions.forEach { option ->
                                    DropdownMenuItem(text = { Text(option, color = Color.White) }, onClick = { countryState = option; countryExpanded = false })
                                }
                            }
                        }

                        AnimatedVisibility(visible = countryState == "Other") {
                            CustomTextField(value = customCountryState, onValueChange = { customCountryState = it }, label = "Specify Country Name", readOnly = !canEditSettings)
                        }

                        CustomTextField(value = stateRegionState, onValueChange = { stateRegionState = it }, label = "State / Territory", readOnly = !canEditSettings)

                        // Language
                        Box(modifier = Modifier.fillMaxWidth()) {
                            CustomTextField(
                                value = languageState, onValueChange = {}, readOnly = true, label = "Preferred Language",
                                trailingIcon = {
                                    if (canEditSettings) IconButton(onClick = { languageExpanded = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray) }
                                }
                            )
                            DropdownMenu(expanded = languageExpanded, onDismissRequest = { languageExpanded = false }, modifier = Modifier.background(cardColor)) {
                                viewModel.languageOptions.forEach { option ->
                                    DropdownMenuItem(text = { Text(option, color = Color.White) }, onClick = { languageState = option; languageExpanded = false })
                                }
                            }
                        }
                    }
                }

                // --- 3. THEME PREFERENCES ---
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = cardColor)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        CustomTextField(
                            value = settingsData.theme.uppercase(), onValueChange = {}, readOnly = true, label = "Visual Theme Mode",
                            trailingIcon = {
                                if (canEditSettings) TextButton(onClick = { themeExpanded = true }) { Text("Adjust", color = accentColor) }
                            }
                        )
                        DropdownMenu(expanded = themeExpanded, onDismissRequest = { themeExpanded = false }, modifier = Modifier.background(cardColor)) {
                            listOf("system", "light", "dark").forEach { mode ->
                                DropdownMenuItem(text = { Text(mode.uppercase(), color = Color.White) }, onClick = {
                                    viewModel.updateSettings(settingsData.copy(theme = mode))
                                    themeExpanded = false
                                })
                            }
                        }
                    }
                }

                // --- 4. ALLERGIES MANAGEMENT ---
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = cardColor)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Allergies Profiles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)

                        if (canEditSettings) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CustomTextField(value = newAllergyText, onValueChange = { newAllergyText = it }, label = "Add Allergy", modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { if (newAllergyText.isNotBlank()) { viewModel.addAllergy(newAllergyText); newAllergyText = "" } },
                                    modifier = Modifier.background(accentColor.copy(alpha = 0.2f), CircleShape)
                                ) { Icon(Icons.Default.Add, contentDescription = "Insert", tint = accentColor) }
                            }
                        }

                        if (settingsData.allergies.isEmpty()) {
                            Text("No allergies recorded.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                settingsData.allergies.forEach { allergy ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(allergy, style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                                        if (canEditSettings) {
                                            Icon(Icons.Default.Close, contentDescription = "Drop", tint = Color.Gray, modifier = Modifier.size(18.dp).clickable { viewModel.removeAllergy(allergy) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 5. API KEYS (Hidden/Read-Only logic) ---
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = cardColor)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("AI Key Integrations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)

                        if (canEditSettings) {
                            // Add New Key Section
                            Box(modifier = Modifier.fillMaxWidth()) {
                                CustomTextField(value = selectedCompany, onValueChange = {}, readOnly = true, label = "Provider Engine", trailingIcon = { IconButton(onClick = { companyExpanded = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray) } })
                                DropdownMenu(expanded = companyExpanded, onDismissRequest = { companyExpanded = false }, modifier = Modifier.background(cardColor)) {
                                    viewModel.aiProviders.forEach { company ->
                                        DropdownMenuItem(text = { Text(company, color = Color.White) }, onClick = { selectedCompany = company; selectedModel = viewModel.modelsMap[company]?.first() ?: ""; companyExpanded = false })
                                    }
                                }
                            }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                CustomTextField(value = selectedModel, onValueChange = {}, readOnly = true, label = "Model Profile Definition", trailingIcon = { IconButton(onClick = { modelExpanded = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray) } })
                                DropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }, modifier = Modifier.background(cardColor)) {
                                    (viewModel.modelsMap[selectedCompany] ?: emptyList()).forEach { mappingModel ->
                                        DropdownMenuItem(text = { Text(mappingModel, color = Color.White) }, onClick = { selectedModel = mappingModel; modelExpanded = false })
                                    }
                                }
                            }
                            CustomTextField(value = apiKeyValueState, onValueChange = { apiKeyValueState = it }, label = "Secure Access Token Key")

                            Button(
                                onClick = { if (apiKeyValueState.isNotBlank()) { viewModel.addApiKey(ApiKeyItem(selectedCompany, selectedModel, apiKeyValueState)); apiKeyValueState = "" } },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) { Text("Map Key Infrastructure") }
                        }

                        // Listed Keys
                        if (settingsData.apiKeys.isEmpty()) {
                            Text("No API keys mapped.", color = Color.Gray, fontSize = 12.sp)
                        } else {
                            settingsData.apiKeys.forEach { integrationKey ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${integrationKey.companyName} » ${integrationKey.modelName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color.White)
                                        Text("•••• •••• " + integrationKey.apiKeyValue.takeLast(4), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                    if (canEditSettings) {
                                        IconButton(onClick = { viewModel.removeApiKey(integrationKey) }) { Icon(Icons.Default.Delete, contentDescription = "Purge", tint = Color(0xFFE53935), modifier = Modifier.size(20.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 6. GLOBAL SAVE BUTTON (Hidden in Guest Mode without Edit access) ---
                if (canEditSettings) {
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
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) {
                        Text("Apply & Sync Server Profile", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// Reusable Dark Mode Text Field Component
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray) },
        readOnly = readOnly,
        modifier = modifier.fillMaxWidth(),
        trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF0A0A12),
            unfocusedContainerColor = Color(0xFF0A0A12),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF1E88E5),
            unfocusedBorderColor = Color.Transparent,
            disabledTextColor = Color.LightGray,
            disabledBorderColor = Color.Transparent,
            disabledContainerColor = Color(0xFF0A0A12)
        ),
        shape = RoundedCornerShape(10.dp)
    )
}