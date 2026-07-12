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
import kotlinx.coroutines.launch
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
            is NutritionScreenState.MealsHistory -> MealsHistoryScreen(viewModel)
            is NutritionScreenState.Goals -> NutritionGoalsScreen(viewModel)
        }
    }
}
@Composable
fun HomeScreen(viewModel: NutritionViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = NutritionUiTokens.SurfaceDark,
                drawerContentColor = Color.White
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("HealthX Nutrition", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
                HorizontalDivider(color = Color.DarkGray)

                NavigationDrawerItem(
                    label = { Text("Nutrition Goals", color = Color.White) },
                    selected = false,
                    onClick = { /* TODO: Navigate to Goals */ },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                NavigationDrawerItem(
                    label = { Text("Nutrition Tracker", color = Color.White) },
                    selected = false,
                    onClick = { /* TODO: Navigate to Tracker */ },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                NavigationDrawerItem(
                    label = { Text("Meals History", color = Color.White) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(NutritionScreenState.MealsHistory)
                    },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = DarkBackground,
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).padding(top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                    Text("Dashboard", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.navigateTo(NutritionScreenState.Scanner) },
                    containerColor = AccentColor,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.CropFree, contentDescription = "Scan Meal")
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
}
@Composable
fun ScannerScreen(viewModel: NutritionViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { viewModel.addImage(it) }
    }

    // Camera Launcher
    var currentCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentCameraUri != null) {
            viewModel.addImage(currentCameraUri!!)
        }
    }

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

        // Image Preview Carousel (FIXED SIZE)
        if (viewModel.selectedImages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No images selected.", color = Color.Gray)
            }
        } else {
            LazyRow(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
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

            // FIXED: Live Camera Action
            IconButton(
                onClick = {
                    val uri = FileUtil.createTempCameraUri(context)
                    currentCameraUri = uri
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier.background(AccentColor, CircleShape).size(72.dp)
            ) {
                Icon(Icons.Default.DocumentScanner, contentDescription = "Camera", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            if (viewModel.selectedImages.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.navigateTo(NutritionScreenState.AmountInput) },
                    modifier = Modifier.background(Color.White, CircleShape).size(56.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Proceed", tint = DarkBackground)
                }
            } else {
                Spacer(modifier = Modifier.size(56.dp))
            }
        }
    }
}

@Composable
fun ImagePreviewCard(uri: Uri, onRemove: () -> Unit) {
    // FIXED: Enforced a strict 100.dp square size so they are previews, not posters
    Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))) {
        AsyncImage(
            model = uri,
            contentDescription = "Meal Preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape).size(24.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun AmountInputScreen(viewModel: NutritionViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current

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
                // FIXED: Process the Uris into real, compressed files before sending
                val actualFiles = viewModel.selectedImages.mapNotNull { uri ->
                    FileUtil.uriToCompressedFile(context, uri)
                }
                viewModel.analyzeMeal(actualFiles)
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

