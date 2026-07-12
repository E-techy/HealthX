package com.example.healthx.nutrition_manager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.healthx.data.model.MealHistoryItem
import kotlinx.coroutines.launch

@Composable
fun MealsHistoryScreen(viewModel: NutritionViewModel) {
    // Fetch data when this screen opens
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
                Text("Meal Log", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {

            // Search Bar
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

            // Date Filters
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

            // Status Filters
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

            // Meals List
            if (viewModel.isFetchingHistory.value) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NutritionUiTokens.AccentColor)
                }
            } else if (viewModel.mealsHistoryList.value.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No meals found in history.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp), // Increased spacing for premium feel
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(viewModel.mealsHistoryList.value) { meal ->
                        HistoryMealCard(meal)
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) } // Bottom padding
                }
            }
        }
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
fun HistoryMealCard(meal: MealHistoryItem) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotation")

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
            // --- HEADER INTERACTION ROW ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meal.mealType ?: "Logged Meal",
                        color = NutritionUiTokens.AccentColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = meal.foodItems?.firstOrNull()?.foodName ?: "Unknown Item",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = meal.date?.take(10) ?: "",
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

            // --- COLLAPSIBLE CONTENT SYSTEM ---
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 16.dp))

                    // Detailed Food Items List with Macro Badges
                    if (!meal.foodItems.isNullOrEmpty()) {
                        Text("Items Logged", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        meal.foodItems.forEach { food ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(food.foodName ?: "Unknown", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }
                                food.amountTaken?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(it, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Integrated Macro Badges
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val weightModifier = Modifier.weight(1f)
                                    MacroBadgeItem(weightModifier, "Cal", food.totalCalories ?: "-", "totalCalories", Color(0xFFFFA500))
                                    MacroBadgeItem(weightModifier, "Pro", food.totalProtein ?: "-", "totalProtein", Color(0xFF4CAF50))
                                    MacroBadgeItem(weightModifier, "Carb", food.totalCarbs ?: "-", "totalCarbs", Color(0xFF2196F3))
                                    MacroBadgeItem(weightModifier, "Fat", food.totalFat ?: "-", "totalFat", Color(0xFFFFEB3B))
                                }
                            }
                        }
                    }

                    // Lazy loaded Images Array
                    if (!meal.imageUrls.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Capture Evidence", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(meal.imageUrls) { url ->
                                AsyncImage(
                                    model = "http://YOUR_SERVER_IP$url",
                                    contentDescription = "Meal Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}