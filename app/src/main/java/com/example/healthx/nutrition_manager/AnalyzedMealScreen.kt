package com.example.healthx.nutrition_manager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthx.data.model.AnalyzeNutritionResponse
import com.example.healthx.data.model.FoodItem

@Composable
fun AnalyzedMealScreen(response: AnalyzeNutritionResponse, viewModel: NutritionViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Meal Logged", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.navigateTo(NutritionScreenState.Home) }) {
                Icon(Icons.Default.Close, contentDescription = "Close to Home", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val items = response.data?.foodItems ?: emptyList()
            items(items) { food ->
                FoodCard(food)
            }
        }
    }
}

@Composable
fun FoodCard(food: FoodItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(food.foodName ?: "Unknown Item", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Text(food.amountTaken ?: "", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MacroBadge("Cal", food.totalCalories ?: "0")
                MacroBadge("Pro", food.totalProtein ?: "0g")
                MacroBadge("Carb", food.totalCarbs ?: "0g")
                MacroBadge("Fat", food.totalFat ?: "0g")
            }

            food.aiInsights?.whyGood?.let { insights ->
                if (insights.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI Insights", color = AccentColor, fontWeight = FontWeight.Bold)
                    insights.forEach { insight ->
                        Text("• $insight", color = Color.LightGray, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MacroBadge(label: String, value: String) {
    Column(
        modifier = Modifier.background(DarkBackground, RoundedCornerShape(8.dp)).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
    }
}