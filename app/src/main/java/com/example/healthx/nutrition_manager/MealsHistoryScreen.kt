package com.example.healthx.nutrition_manager

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.healthx.BuildConfig
import com.example.healthx.data.local.SessionManager
import com.example.healthx.data.model.FoodItem
import com.example.healthx.data.model.MealHistoryItem
import kotlinx.coroutines.launch

@Composable
fun MealsHistoryScreen(viewModel: NutritionViewModel) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val delegatedSession by sessionManager.delegatedSessionFlow.collectAsState()

    // State for Full Screen Image Viewer
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchMealsHistory()
    }

    Scaffold(
        containerColor = NutritionUiTokens.DarkBackground,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(NutritionScreenState.Home) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Meal Log Archive", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {

            // --- SEARCH BAR ---
            OutlinedTextField(
                value = viewModel.searchMealId.value,
                onValueChange = { viewModel.searchMealId.value = it },
                placeholder = { Text("Search by Meal ID...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, tint = Color.Gray, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NutritionUiTokens.SurfaceDark,
                    unfocusedContainerColor = NutritionUiTokens.SurfaceDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NutritionUiTokens.AccentColor,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- FILTERS ---
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val dates = listOf("Today", "This Week", "This Month")
                items(dates) { filter ->
                    FilterChipUI(
                        label = filter,
                        isSelected = viewModel.selectedDateFilter.value == filter,
                        onClick = {
                            viewModel.selectedDateFilter.value = filter
                            viewModel.fetchMealsHistory()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val statuses = listOf("All", "Active", "Discarded")
                items(statuses) { filter ->
                    FilterChipUI(
                        label = filter,
                        isSelected = viewModel.selectedStatusFilter.value == filter,
                        onClick = {
                            viewModel.selectedStatusFilter.value = filter
                            viewModel.fetchMealsHistory()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- MEALS LIST ---
            if (viewModel.isFetchingHistory.value) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NutritionUiTokens.AccentColor)
                }
            } else if (viewModel.mealsHistoryList.value.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No meals found in the archives.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(viewModel.mealsHistoryList.value) { meal ->
                        HistoryMealCard(
                            meal = meal,
                            onImageClick = { url -> selectedImageUrl = url }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    // --- FULL SCREEN IMAGE VIEWER OVERLAY ---
    if (selectedImageUrl != null) {
        FullScreenImageViewer(
            imageUrl = selectedImageUrl!!,
            context = context,
            onDismiss = { selectedImageUrl = null }
        )
    }
}

@Composable
fun FilterChipUI(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (isSelected) NutritionUiTokens.AccentColor else NutritionUiTokens.SurfaceDark,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) NutritionUiTokens.DarkBackground else Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun HistoryMealCard(meal: MealHistoryItem, onImageClick: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotation")

    // DYNAMIC TITLE LOGIC: Combine all food names, fallback if empty
    val combinedFoodTitle = meal.foodItems?.mapNotNull { it.foodName }?.takeIf { it.isNotEmpty() }?.joinToString(" + ") ?: "Analyzed Meal"
    val totalCals = meal.foodItems?.mapNotNull { extractNumber(it.totalCalories ?: "0").toInt() }?.sum() ?: 0

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .background(NutritionUiTokens.CardGradient)
                .padding(20.dp)
        ) {
            // --- HEADER ROW ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${meal.mealType ?: "Logged Meal"} • $totalCals kcal",
                        color = NutritionUiTokens.AccentColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = combinedFoodTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = meal.date?.replace("T", " ")?.take(16) ?: "",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (meal.discarded) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFF7B7B).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFFF7B7B).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Discarded", color = Color(0xFFFF7B7B), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = Color.Gray,
                        modifier = Modifier.rotate(rotationState)
                    )
                }
            }

            // --- FULL DATA DISPLAY (EXPANDED) ---
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 16.dp))

                    if (!meal.foodItems.isNullOrEmpty()) {
                        meal.foodItems.forEach { food ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                // Food Name & Score
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(food.foodName ?: "Unknown", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    food.foodScore?.let { score ->
                                        Row(
                                            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = "Score", tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(score.toString(), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // DYNAMIC DIETARY BADGES (Vegan, Veg, Organic)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    DietaryBadge(food.mealCategory)
                                    if (food.isOrganic == true) {
                                        Surface(color = Color(0xFF81C784).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.5f))) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                Icon(Icons.Default.Eco, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("100% Organic", color = Color(0xFF81C784), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Core Macros
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val weightModifier = Modifier.weight(1f)
                                    MacroBadgeItem(weightModifier, "Cal", food.totalCalories ?: "-", "totalCalories", Color(0xFFFFA500))
                                    MacroBadgeItem(weightModifier, "Pro", food.totalProtein ?: "-", "totalProtein", Color(0xFF4CAF50))
                                    MacroBadgeItem(weightModifier, "Carb", food.totalCarbs ?: "-", "totalCarbs", Color(0xFF2196F3))
                                    MacroBadgeItem(weightModifier, "Fat", food.totalFat ?: "-", "totalFat", Color(0xFFFFEB3B))
                                }

                                // Secondary Macros (Sat Fat, Unsat Fat, Water)
                                if (food.saturatedFat != null || food.unsaturatedFat != null || food.totalWater != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val weightModifier = Modifier.weight(1f)
                                        if (food.saturatedFat != null) MacroBadgeItem(weightModifier, "Sat Fat", food.saturatedFat, "saturatedFat", Color(0xFFCFD8DC))
                                        if (food.unsaturatedFat != null) MacroBadgeItem(weightModifier, "Unsat Fat", food.unsaturatedFat, "unsaturatedFat", Color(0xFF90A4AE))
                                        if (food.totalWater != null) MacroBadgeItem(weightModifier, "Water", food.totalWater, "totalWater", Color(0xFF29B6F6))
                                    }
                                }

                                // AI Insights
                                if (food.aiInsights?.whyGood?.isNotEmpty() == true || food.aiInsights?.whyNot?.isNotEmpty() == true) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("AI Insights", color = NutritionUiTokens.AccentColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    food.aiInsights.whyGood?.forEach { text -> InsightRowItem(icon = Icons.Default.CheckCircle, color = Color(0xFF81C784), text = text) }
                                    food.aiInsights.whyNot?.forEach { text -> InsightRowItem(icon = Icons.Default.Cancel, color = Color(0xFFE57373), text = text) }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Collapsible Sections
                                if (!food.ingredients.isNullOrEmpty()) {
                                    CollapsiblePanelSection("Ingredients", food.ingredients.joinToString(", "))
                                }
                                if (!food.chemicalsOrPreservatives.isNullOrEmpty()) {
                                    CollapsiblePanelSection("Additives & Preservatives", food.chemicalsOrPreservatives.joinToString(", "), isThreat = true)
                                }
                            }
                        }
                    }

                    // CAPTURED MEDIA THUMBNAILS (Clickable for Full Screen)
                    if (!meal.imageUrls.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Captured Media", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(meal.imageUrls) { url ->
                                val baseUrl = BuildConfig.BASE_URL.removeSuffix("/")
                                val fullImageUrl = if (url.startsWith("http")) url else "$baseUrl$url"

                                AsyncImage(
                                    model = fullImageUrl,
                                    contentDescription = "Meal Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .clickable { onImageClick(fullImageUrl) } // TRIGGER FULL SCREEN
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DietaryBadge(category: String?) {
    val (color, icon, text) = when (category?.uppercase()) {
        "VEGAN" -> Triple(Color(0xFF00E676), Icons.Default.Yard, "Vegan")
        "VEG" -> Triple(Color(0xFF4CAF50), Icons.Default.Eco, "Vegetarian")
        "NON_VEG" -> Triple(Color(0xFFE53935), Icons.Default.SetMeal, "Non-Veg")
        else -> return // Don't render a badge if unknown
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// FULL SCREEN CINEMATIC IMAGE VIEWER & DOWNLOADER
// ==========================================
@Composable
fun FullScreenImageViewer(imageUrl: String, context: Context, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // Takes up whole screen
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Main Image
            AsyncImage(
                model = imageUrl,
                contentDescription = "Full Screen Media",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Top Control Bar (Close & Download)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                IconButton(
                    onClick = { downloadImage(context, imageUrl) },
                    modifier = Modifier.background(NutritionUiTokens.AccentColor.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Download Image", tint = Color.White)
                }
            }
        }
    }
}

// Utility Function: Downloads the image via Android's Native DownloadManager
fun downloadImage(context: Context, url: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Nutrition Media")
            .setDescription("Downloading analyzed meal image...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "HealthX_Meal_${System.currentTimeMillis()}.jpg")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to download image.", Toast.LENGTH_SHORT).show()
    }
}