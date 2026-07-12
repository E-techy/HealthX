package com.example.healthx.nutrition_manager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
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
import com.example.healthx.data.model.MealHistoryItem
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.ArrowBack // Added this line
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search

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
                    Text("No meals found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(viewModel.mealsHistoryList.value) { meal ->
                        HistoryMealCard(meal)
                    }
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

    Card(
        modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = NutritionUiTokens.SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Basic Info (Always visible)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = meal.foodItems?.firstOrNull()?.foodName ?: "Unknown Meal",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (meal.discarded) {
                    Text("Discarded", color = Color(0xFFFF7B7B), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(text = meal.date?.take(10) ?: "", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

            // Expanded Info (Images load ONLY when this opens)
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = Color.DarkGray, modifier = Modifier.padding(bottom = 12.dp))

                    meal.foodItems?.forEach { food ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• ${food.foodName}", color = Color.LightGray)
                            Text(food.totalCalories ?: "-", color = NutritionUiTokens.AccentColor)
                        }
                    }

                    // Lazy load images from network only when expanded
                    if (!meal.imageUrls.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(meal.imageUrls) { url ->
                                // Note: In a real app, prepend your BASE_URL to this 'url' if the API returns a relative path
                                AsyncImage(
                                    model = "http://YOUR_SERVER_IP$url",
                                    contentDescription = "Meal Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}