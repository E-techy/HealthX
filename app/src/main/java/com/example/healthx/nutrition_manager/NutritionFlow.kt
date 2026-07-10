package com.example.healthx.nutrition_manager

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

// --- Theme Colors ---
val DarkBackground = Color(0xFF0D0D0D)
val SurfaceDark = Color(0xFF1A1A1A)
val AccentColor = Color(0xFF6200EE) // A deep cinematic purple/blue accent

@Composable
fun NutritionManagerApp(viewModel: NutritionViewModel) {
    Surface(color = DarkBackground, modifier = Modifier.fillMaxSize()) {
        when (val state = viewModel.currentScreen.value) {
            is NutritionScreenState.Home -> HomeScreen(viewModel)
            is NutritionScreenState.Scanner -> ScannerScreen(viewModel)
            is NutritionScreenState.AmountInput -> AmountInputScreen(viewModel)
            is NutritionScreenState.Loading -> LoadingScreen()
            is NutritionScreenState.Error -> ErrorScreen(state.message, viewModel)
            is NutritionScreenState.Success -> AnalyzedMealScreen(state.data, viewModel)
        }
    }
}

@Composable
fun HomeScreen(viewModel: NutritionViewModel) {
    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(NutritionScreenState.Scanner) },
                containerColor = AccentColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Scan Meal")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Nutrition Manager", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text("Tap the scanner to log a meal", color = Color.Gray)
        }
    }
}

@Composable
fun ScannerScreen(viewModel: NutritionViewModel) {
    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { viewModel.addImage(it) }
    }

    // Top Bar with Back and Close
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { viewModel.navigateTo(NutritionScreenState.Home) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            IconButton(onClick = { viewModel.navigateTo(NutritionScreenState.Home) }) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Scan Meal", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Image Preview Carousel
        if (viewModel.selectedImages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No images selected.", color = Color.Gray)
            }
        } else {
            LazyRow(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(viewModel.selectedImages) { uri ->
                    ImagePreviewCard(uri = uri, onRemove = { viewModel.removeImage(uri) })
                }
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.background(SurfaceDark, CircleShape).size(56.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
            }

            // In a real app, you'd trigger Camera capture here. Using a placeholder button for layout.
            IconButton(
                onClick = { /* Trigger Camera Intent Here */ },
                modifier = Modifier.background(AccentColor, CircleShape).size(72.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            // Proceed Arrow (Only visible if images exist)
            if (viewModel.selectedImages.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.navigateTo(NutritionScreenState.AmountInput) },
                    modifier = Modifier.background(Color.White, CircleShape).size(56.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Proceed", tint = DarkBackground)
                }
            } else {
                Spacer(modifier = Modifier.size(56.dp)) // Maintain spacing
            }
        }
    }
}

@Composable
fun ImagePreviewCard(uri: Uri, onRemove: () -> Unit) {
    Box(modifier = Modifier.width(200.dp).fillMaxHeight(0.8f).clip(RoundedCornerShape(16.dp))) {
        AsyncImage(
            model = uri,
            contentDescription = "Meal Preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
        }
    }
}

@Composable
fun AmountInputScreen(viewModel: NutritionViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { viewModel.navigateTo(NutritionScreenState.Scanner) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            IconButton(onClick = { viewModel.navigateTo(NutritionScreenState.Home) }) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Context is everything.", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text("How much did you eat? Did you leave anything on the plate?", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = viewModel.mealAmountInput.value,
            onValueChange = { viewModel.mealAmountInput.value = it },
            placeholder = { Text("e.g., I ate half of it and drank all the coke", color = Color.DarkGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = AccentColor,
                unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth().height(150.dp),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                // TODO: In your actual Activity, map your Uri list to actual java.io.File objects here
                val fakeFileList = emptyList<File>()
                viewModel.analyzeMeal(fakeFileList)
            },
            colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Analyze Meal", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AccentColor)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Vision AI Processing...", color = Color.Gray)
        }
    }
}

@Composable
fun ErrorScreen(errorMsg: String, viewModel: NutritionViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Analysis Failed", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text(errorMsg, color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))

        Button(
            onClick = { viewModel.navigateTo(NutritionScreenState.AmountInput) },
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
        ) {
            Text("Go Back & Edit", color = Color.White)
        }
    }
}