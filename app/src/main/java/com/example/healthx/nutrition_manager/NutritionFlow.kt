package com.example.healthx.nutrition_manager

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.healthx.data.local.SessionManager
import com.example.healthx.data.model.MealHistoryItem
import com.example.healthx.data.model.NutritionGoal
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

// --- Theme Colors ---
val DarkBackground = Color(0xFF0D0D0D)
val SurfaceDark = Color(0xFF1A1A1A)
val AccentColor = Color(0xFF6200EE)

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
            is NutritionScreenState.CreateGoal -> CreateGoalScreen(viewModel)
        }
    }
}

@Composable
fun HomeScreen(viewModel: NutritionViewModel) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val delegatedSession by sessionManager.delegatedSessionFlow.collectAsState()

    // --- PERMISSION CHECKS ---
    val isGuest = delegatedSession != null
    val canSeeNutrition = !isGuest || delegatedSession!!.hasPermission("SEE_NUTRITION")
    val canEditNutrition = !isGuest || delegatedSession!!.hasPermission("EDIT_NUTRITION")
    val canSeeGoals = !isGuest || delegatedSession!!.hasPermission("SEE_GOALS")

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Fetch data for the dashboard on load
    LaunchedEffect(Unit) {
        if (canSeeGoals) viewModel.fetchGoals()
        if (canSeeNutrition) viewModel.fetchMealsHistory()
    }

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

                if (canSeeGoals) {
                    NavigationDrawerItem(
                        label = { Text("Nutrition Goals", color = Color.White) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(NutritionScreenState.Goals)
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )
                }

                if (canEditNutrition) {
                    NavigationDrawerItem(
                        label = { Text("AI Vision Scanner", color = Color.White) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(NutritionScreenState.Scanner)
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )
                }

                if (canSeeNutrition) {
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
                    Text("Nutrition Hub", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
            },
            floatingActionButton = {
                // HIDE THE SCANNER FAB IF THEY LACK EDIT PERMISSIONS
                if (canEditNutrition) {
                    FloatingActionButton(
                        onClick = { viewModel.navigateTo(NutritionScreenState.Scanner) },
                        containerColor = NutritionUiTokens.AccentColor,
                        contentColor = NutritionUiTokens.DarkBackground,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp).border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        // UPGRADED SCANNER ICON
                        Icon(Icons.Default.CameraEnhance, contentDescription = "AI Vision Scan", modifier = Modifier.size(32.dp))
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 100.dp, start = 16.dp, end = 16.dp)
            ) {

                // 1. THE DELEGATED GUEST BANNER
                if (isGuest) {
                    item {
                        Surface(
                            color = Color(0xFFE53935).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Delegated Access Active", color = Color(0xFFE53935), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Viewing ${delegatedSession!!.name}'s Nutrition Profile.", color = Color.White, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { sessionManager.exitDelegatedMode() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Exit", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 2. DAILY GOALS CHART (Concentric Progress)
                if (canSeeGoals) {
                    item {
                        val activeGoal = viewModel.goalsList.value.find { it.isActive }
                        DashboardMacroChart(activeGoal)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // 3. QUICK LOGGING WIDGETS (Water)
                if (canEditNutrition) {
                    item {
                        DashboardHydrationWidget()
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // 4. RECENT MEALS LEDGER
                if (canSeeNutrition) {
                    item {
                        Text("Recent Telemetry", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        val recentMeals = viewModel.mealsHistoryList.value.take(3)

                        if (recentMeals.isEmpty() && !viewModel.isFetchingHistory.value) {
                            Box(
                                modifier = Modifier.fillMaxWidth().background(NutritionUiTokens.SurfaceDark, RoundedCornerShape(16.dp)).padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No recent meals logged.", color = Color.Gray)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                recentMeals.forEach { meal ->
                                    CompactMealCard(meal) {
                                        viewModel.navigateTo(NutritionScreenState.MealsHistory)
                                    }
                                }
                            }
                        }

                        if (viewModel.mealsHistoryList.value.size > 3) {
                            TextButton(
                                onClick = { viewModel.navigateTo(NutritionScreenState.MealsHistory) },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Text("View Full History", color = NutritionUiTokens.AccentColor)
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// ADVANCED UI DASHBOARD COMPONENTS
// ==========================================

@Composable
fun DashboardMacroChart(goal: NutritionGoal?) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = NutritionUiTokens.SurfaceDark),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Today's Trajectory", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(24.dp))

            if (goal == null) {
                Icon(Icons.Default.QueryStats, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No active protocol detected.", color = Color.Gray)
            } else {
                val todayString = SimpleDateFormat("yyyy-MM-dd", LocalLocale.current.platformLocale).format(Date())
                val todayProgress = goal.progressChart?.find { it.date == todayString }

                // Map target logic to progress values
                val ringsData = goal.targets.mapNotNull { target ->
                    val progress = todayProgress?.nutrientProgress?.find { it.nutrientName == target.nutrientName }?.amountCompleted ?: "0"
                    val currentVal = extractNumber(progress)
                    val targetVal = extractNumber(target.targetAmount)
                    if (targetVal > 0) {
                        val color = when (target.nutrientName.lowercase()) {
                            "calories" -> Color(0xFFFFA500)
                            "protein" -> Color(0xFF4CAF50)
                            "carbs" -> Color(0xFF29B6F6)
                            "fat" -> Color(0xFFFFEB3B)
                            else -> NutritionUiTokens.AccentColor
                        }
                        Triple(target.nutrientName, (currentVal / targetVal).coerceAtMost(1f), color)
                    } else null
                }.take(4) // Max 4 rings for visual sanity

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                    // CONCENTRIC RINGS
                    Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                        ringsData.forEachIndexed { index, data ->
                            AnimatedConcentricRing(
                                percentage = data.second,
                                color = data.third,
                                // Decrease radius for each subsequent inner ring
                                paddingOffset = (index * 16).dp
                            )
                        }
                    }

                    // LEGEND
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ringsData.forEach { data ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(data.third, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(data.first, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedConcentricRing(percentage: Float, color: Color, paddingOffset: androidx.compose.ui.unit.Dp) {
    val animatedProgress by animateFloatAsState(targetValue = percentage, animationSpec = tween(1500), label = "ring")
    Canvas(modifier = Modifier.fillMaxSize().padding(paddingOffset)) {
        val strokeWidth = 8.dp.toPx()
        drawArc(color = color.copy(alpha = 0.15f), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
        drawArc(color = color, startAngle = -90f, sweepAngle = animatedProgress * 360f, useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
    }
}

@Composable
fun DashboardHydrationWidget() {
    var waterGlasses by remember { mutableStateOf(0) } // In a real app, bind to ViewModel/Backend

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NutritionUiTokens.SurfaceDark),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).background(Color(0xFF29B6F6).copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Opacity, contentDescription = "Water", tint = Color(0xFF29B6F6))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Hydration", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("${waterGlasses * 250} ml logged today", color = Color.Gray, fontSize = 12.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = { if (waterGlasses > 0) waterGlasses-- },
                    modifier = Modifier.background(NutritionUiTokens.DarkBackground, CircleShape).size(36.dp)
                ) { Icon(Icons.Default.Remove, tint = Color.White, contentDescription = "Minus") }

                Text("$waterGlasses", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                IconButton(
                    onClick = { waterGlasses++ },
                    modifier = Modifier.background(Color(0xFF29B6F6), CircleShape).size(36.dp)
                ) { Icon(Icons.Default.Add, tint = Color.Black, contentDescription = "Plus") }
            }
        }
    }
}

@Composable
fun CompactMealCard(meal: MealHistoryItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NutritionUiTokens.SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(modifier = Modifier.size(50.dp).background(NutritionUiTokens.DarkBackground, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            val url = meal.imageUrls?.firstOrNull()
            if (url != null) {
                val fullUrl = "${com.example.healthx.BuildConfig.BASE_URL.removeSuffix("/")}$url"
                AsyncImage(model = fullUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
            } else {
                Icon(Icons.Default.Restaurant, tint = Color.Gray, contentDescription = null)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            val title = meal.foodItems?.firstOrNull()?.foodName ?: "Logged Meal"
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${meal.mealType} • ${meal.date?.take(10)}", color = Color.Gray, fontSize = 12.sp)
        }

        // Calories Badge
        val cals = meal.foodItems?.firstOrNull()?.totalCalories ?: "-"
        Box(modifier = Modifier.background(Color(0xFFFFA500).copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(cals, color = Color(0xFFFFA500), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

