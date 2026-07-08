package com.example.healthx.ui.screens.nutrition

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.core.content.FileProvider
import com.example.healthx.ui.theme.HealthXTheme
import java.io.File

class NutritionActivity : ComponentActivity() {
    private val viewModel: NutritionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthXTheme {
                NutritionScreenRoot(viewModel, onBack = { finish() })
            }
        }
    }
}

@Composable
fun NutritionScreenRoot(viewModel: NutritionViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // State for Portions & URIs
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var showPortionDialog by remember { mutableStateOf(false) }

    // Camera Launcher
    val cameraUri = remember {
        val tempFile = File(context.cacheDir, "camera_temp.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            selectedUri = cameraUri
            showPortionDialog = true
        }
    }

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            showPortionDialog = true
        }
    }

    Scaffold(
        containerColor = Color(0xFF09090B),
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Nutrition Center", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, tint = Color.White, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF09090B))
            )
        },
        floatingActionButton = {
            if (uiState is NutritionState.Dashboard) {
                Column(horizontalAlignment = Alignment.End) {
                    SmallFloatingActionButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        containerColor = Color(0xFF1E1E24),
                        contentColor = Color.White
                    ) { Icon(Icons.Default.PhotoLibrary, "Gallery") }

                    Spacer(modifier = Modifier.height(12.dp))

                    FloatingActionButton(
                        onClick = { cameraLauncher.launch(cameraUri) },
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.Black
                    ) { Icon(Icons.Default.CameraAlt, "Scan Food") }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (val state = uiState) {
                is NutritionState.Loading -> CircularProgressIndicator(color = Color(0xFF4CAF50), modifier = Modifier.align(Alignment.Center))
                is NutritionState.Error -> Text(state.message, color = Color(0xFFE53935), modifier = Modifier.align(Alignment.Center))
                is NutritionState.Dashboard -> DashboardContent(state.summary)
                is NutritionState.AiReview -> AiReviewCard(
                    data = state.analysisData,
                    onAccept = { viewModel.consumeAndTrackMeal(state.analysisData) },
                    onDiscard = { viewModel.cancelAiReview() }
                )
            }
        }
    }

    if (showPortionDialog) {
        PortionInputDialog(
            onConfirm = { quantity ->
                showPortionDialog = false
                selectedUri?.let { viewModel.analyzeImage(context, it, quantity) }
            },
            onDismiss = { showPortionDialog = false }
        )
    }
}

@Composable
fun DashboardContent(summary: DailySummary) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Main Health Score Banner
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF12121A)).padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Daily Score", color = Color.Gray, fontSize = 14.sp)
                Text("${summary.dailyHealthScore}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Black, fontSize = 42.sp)
            }
            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(56.dp))
        }

        Text("Macros", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MacroBox("Calories", "${summary.totalCalories} kcal", Color(0xFFFF9800), Modifier.weight(1f))
            MacroBox("Protein", "${summary.totalProtein} g", Color(0xFF2196F3), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MacroBox("Carbs", "${summary.totalCarbs} g", Color(0xFF9C27B0), Modifier.weight(1f))
            MacroBox("Fat", "${summary.totalFat} g", Color(0xFFE53935), Modifier.weight(1f))
        }
    }
}

@Composable
fun MacroBox(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF16161E)).padding(16.dp)
    ) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.height(12.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(title, color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun AiReviewCard(data: AiFoodData, onAccept: () -> Unit, onDiscard: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(Color(0xFF12121A)).padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("AI Analysis Complete", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(data.foodDetected, color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
            Text("Category: ${data.foodCategory} • Portion: ${data.portionAnalyzed}g", color = Color.Gray, fontSize = 14.sp)

            Divider(color = Color(0xFF2A2A35))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Calories: ${data.rawNutrientsExtracted.calories}", color = Color.White)
                Text("Protein: ${data.rawNutrientsExtracted.protein}g", color = Color.White)
            }

            if (!data.allergens.isNullOrEmpty()) {
                Text("⚠️ Allergens: ${data.allergens.joinToString()}", color = Color(0xFFFF9800), fontWeight = FontWeight.Medium)
            }

            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E1E24)).padding(16.dp)) {
                Text(data.aiInsights, color = Color.LightGray, fontSize = 15.sp, lineHeight = 22.sp)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onDiscard,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A35)),
                modifier = Modifier.weight(1f)
            ) { Text("Discard", color = Color.White) }

            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier.weight(1f)
            ) { Text("Eat & Track", color = Color.Black, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun PortionInputDialog(onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var portionText by remember { mutableStateOf("100") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF16161E),
        title = { Text("Enter Quantity", color = Color.White) },
        text = {
            OutlinedTextField(
                value = portionText,
                onValueChange = { portionText = it },
                label = { Text("Quantity in grams/ml", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4CAF50)
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(portionText.toIntOrNull() ?: 100) }) {
                Text("Analyze", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}