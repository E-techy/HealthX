package com.example.healthx.nutrition_manager

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.healthx.data.model.AnalyzeNutritionResponse
import com.example.healthx.data.model.FoodItem

@Composable
fun AnalyzedMealScreen(response: AnalyzeNutritionResponse, viewModel: NutritionViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Meal Analysis", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                response.data?.mealType?.let {
                    Text(it, color = AccentColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(
                onClick = { viewModel.navigateTo(NutritionScreenState.Home) },
                modifier = Modifier.background(SurfaceDark, CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close to Home", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            val items = response.data?.foodItems ?: emptyList()

            if (items.isEmpty()) {
                item {
                    Text("No food items detected.", color = Color.Gray)
                }
            }

            items(items) { food ->
                FoodCard(food)
            }

            // Render the images sent at the very bottom
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Images Analyzed", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Using the local images we already have in the viewmodel so we don't need to re-download
                    items(viewModel.selectedImages) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Analyzed Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp)) // Bottom padding
            }
        }
    }
}

@Composable
fun FoodCard(food: FoodItem) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(true) } // Main card toggle

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // --- HEADER (Always Visible) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Category Icon (Veg/NonVeg)
                    CategoryIcon(food.mealCategory)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = food.foodName ?: "Unknown Item",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Food Score
                food.foodScore?.let { score ->
                    Row(
                        modifier = Modifier.background(DarkBackground, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Score", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(score.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- EXPANDABLE BODY ---
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {

                    // Main Macros
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MacroBadge("Cal", food.totalCalories ?: "-", Icons.Default.LocalFireDepartment, Color(0xFFFFA500))
                        MacroBadge("Pro", food.totalProtein ?: "-", Icons.Default.FitnessCenter, Color(0xFF4CAF50))
                        MacroBadge("Carb", food.totalCarbs ?: "-", Icons.Default.BreakfastDining, Color(0xFF2196F3))
                        MacroBadge("Fat", food.totalFat ?: "-", Icons.Default.WaterDrop, Color(0xFFFFEB3B))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // AI Insights (Pros / Cons)
                    if (food.aiInsights?.whyGood?.isNotEmpty() == true || food.aiInsights?.whyNot?.isNotEmpty() == true) {
                        Text("AI Insights", color = AccentColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        food.aiInsights.whyGood?.forEach { pros ->
                            InsightRow(icon = Icons.Default.CheckCircle, color = Color.Green, text = pros)
                        }
                        food.aiInsights.whyNot?.forEach { cons ->
                            InsightRow(icon = Icons.Default.Cancel, color = Color.Red, text = cons)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Collapsible Sub-Sections
                    if (!food.ingredients.isNullOrEmpty()) {
                        CollapsibleSection("Ingredients", food.ingredients.joinToString(", "))
                    }
                    if (!food.allergens.isNullOrEmpty()) {
                        CollapsibleSection("Allergens Warning", food.allergens.joinToString(", "), isWarning = true)
                    }
                    if (!food.otherNutrients.isNullOrEmpty()) {
                        val nutrientsText = food.otherNutrients.joinToString(", ") { "${it.name}: ${it.amount}" }
                        CollapsibleSection("Other Nutrients", nutrientsText)
                    }

                    // Extra Info (Brand, Expiry, etc) - Only show if at least one exists
                    if (food.brandName != null || food.expiryDate != null || food.countryOfOrigin != null) {
                        val extraText = buildString {
                            food.brandName?.let { append("Brand: $it\n") }
                            food.countryOfOrigin?.let { append("Origin: $it\n") }
                            food.manufactureDate?.let { append("Mfg: $it\n") }
                            food.expiryDate?.let { append("Exp: $it") }
                        }.trim()
                        CollapsibleSection("Product Details", extraText)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons (Quantity Selection)
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        food.amountTaken?.let { amount ->
                            if (amount.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { Toast.makeText(context, "Saving your quantity: $amount", Toast.LENGTH_SHORT).show() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Text("Proceed with My Quantity ($amount)")
                                }
                            }
                        }

                        food.aiRecommendedQuantity?.let { recAmount ->
                            if (recAmount.isNotBlank()) {
                                Button(
                                    onClick = { Toast.makeText(context, "Applying AI optimal quantity: $recAmount", Toast.LENGTH_SHORT).show() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                                ) {
                                    Text("Use AI Recommended ($recAmount)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- HELPER COMPOSABLES ---

@Composable
fun CategoryIcon(category: String?) {
    val (color, icon) = when (category?.uppercase()) {
        "VEG" -> Color.Green to Icons.Default.Eco
        "NON_VEG" -> Color.Red to Icons.Default.SetMeal
        "VEGAN" -> Color(0xFF00FF00) to Icons.Default.Yard
        else -> Color.Gray to Icons.Default.HelpOutline
    }

    Box(
        modifier = Modifier.size(32.dp).background(color.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = category, tint = color, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun MacroBadge(label: String, value: String, icon: ImageVector, tint: Color) {
    Column(
        modifier = Modifier.background(DarkBackground, RoundedCornerShape(12.dp)).padding(12.dp).width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(label, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun InsightRow(icon: ImageVector, color: Color, text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CollapsibleSection(title: String, content: String, isWarning: Boolean = false) {
    var isExpanded by remember { mutableStateOf(false) }
    val titleColor = if (isWarning) Color(0xFFff6b6b) else Color.White

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = titleColor, fontWeight = FontWeight.Bold)
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Toggle",
                tint = Color.Gray
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Text(content, color = Color.Gray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}