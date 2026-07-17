package com.example.healthx.nutrition_manager

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.healthx.R // Replace with your actual resource package
import com.example.healthx.data.model.AnalyzeNutritionResponse
import com.example.healthx.data.model.FoodItem

// --- CENTRALIZED CONFIGURATION & DESIGN TOKENS ---
object NutritionUiTokens {
    val DarkBackground = Color(0xFF0B0C10)
    val SurfaceDark = Color(0xFF1F2833)
    val AccentColor = Color(0xFF66FCF1)
    val AccentDim = Color(0xFF45A29E)

    // Smooth gradients for high-end cinematic visual aesthetics
    val CardGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF242C37), Color(0xFF171E24))
    )
}

// Sealed class for versatile icon mapping (supports both standard vectors and custom SVGs)
sealed class NutritionIcon {
    data class Vector(val imageVector: ImageVector) : NutritionIcon()
    data class Resource(val resId: Int) : NutritionIcon()
}

/**
 * Global Mapping Registry for Extended Macros and Meal Categories.
 * Replace R.drawable hooks with your specific asset resource IDs once imported.
 */
object NutritionIconRegistry {
    fun getMacroIcon(macroKey: String): NutritionIcon {
        return when (macroKey) {
            "totalCalories" -> NutritionIcon.Vector(Icons.Default.LocalFireDepartment) // Target SVG: R.drawable.ic_fire_designer
            "totalProtein" -> NutritionIcon.Vector(Icons.Default.FitnessCenter)      // Target SVG: R.drawable.ic_protein_designer
            "totalCarbs" -> NutritionIcon.Vector(Icons.Default.BreakfastDining)     // Target SVG: R.drawable.ic_carbs_designer
            "totalFat" -> NutritionIcon.Vector(Icons.Default.WaterDrop)             // Target SVG: R.drawable.ic_fat_designer
            "saturatedFat" -> NutritionIcon.Vector(Icons.Default.Layers)             // Target SVG: R.drawable.ic_sat_fat
            "unsaturatedFat" -> NutritionIcon.Vector(Icons.Default.Waves)            // Target SVG: R.drawable.ic_unsat_fat
            "totalWater" -> NutritionIcon.Vector(Icons.Default.Opacity)              // Target SVG: R.drawable.ic_water_glass
            else -> NutritionIcon.Vector(Icons.Default.HelpOutline)
        }
    }

    fun getCategoryConfig(category: String?): Triple<Color, ImageVector, String> {
        return when (category?.uppercase()) {
            "VEG" -> Triple(Color(0xFF4CAF50), Icons.Default.Eco, "VEG")
            "NON_VEG" -> Triple(Color(0xFFE53935), Icons.Default.SetMeal, "NON-VEG")
            "VEGAN" -> Triple(Color(0xFF00E676), Icons.Default.Yard, "VEGAN")
            else -> Triple(Color.Gray, Icons.Default.HelpOutline, "UNKNOWN")
        }
    }
}

@Composable
fun AnalyzedMealScreen(response: AnalyzeNutritionResponse, viewModel: NutritionViewModel) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = NutritionUiTokens.DarkBackground
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp)) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Meal Analysis",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    response.data?.mealType?.let {
                        Text(
                            text = it,
                            color = NutritionUiTokens.AccentColor,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(
                    onClick = { viewModel.navigateTo(NutritionScreenState.Home) },
                    modifier = Modifier.background(NutritionUiTokens.SurfaceDark, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                val items = response.data?.foodItems ?: emptyList()

                if (items.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("No food items identified.", color = Color.Gray)
                        }
                    }
                }

                items(items) { food ->
                    FoodCard(food)
                }

                // Gallery Overlay Section at the Bottom
                if (!viewModel.selectedImages.isNullOrEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzed Media Capture",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(viewModel.selectedImages) { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Source Captured Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

// ... inside AnalyzedMealScreen.kt ...

@Composable
fun FoodCard(food: FoodItem) {
    val context = LocalContext.current
    val sessionManager = remember { com.example.healthx.data.local.SessionManager(context) }
    val delegatedSession by sessionManager.delegatedSessionFlow.collectAsState()

    val isGuest = delegatedSession != null
    val canEditNutrition = !isGuest || delegatedSession!!.hasPermission("EDIT_NUTRITION")

    var isExpanded by remember { mutableStateOf(true) }
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotation")

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .background(NutritionUiTokens.CardGradient)
                .padding(20.dp)
        ) {
            // ... (Keep the Header and Collapsible sections exactly the same until the buttons) ...

            // Product Details Box Factory
            if (food.brandName != null || food.expiryDate != null || food.countryOfOrigin != null || food.nutritionValuePerUnit != null) {
                val buildDetails = buildString {
                    food.brandName?.let { append("Brand Label: $it\n") }
                    food.nutritionValuePerUnit?.let { append("Metric Unit Base: $it\n") }
                    food.countryOfOrigin?.let { append("Origin Domain: $it\n") }
                    food.manufactureDate?.let { append("Production Date: $it\n") }
                    food.expiryDate?.let { append("Terminal Lifecycle Date: $it") }
                }.trim()
                CollapsiblePanelSection("Production Logs & Metadata", buildDetails)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- STRATEGIC PERMISSION-CONTROLLED BUTTONS ---
            if (canEditNutrition) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    food.amountTaken?.let { amt ->
                        if (amt.isNotBlank()) {
                            OutlinedButton(
                                onClick = { Toast.makeText(context, "Executing Logging Payload: $amt", Toast.LENGTH_SHORT).show() },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Text("Log Base User Capture Amount ($amt)", fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    food.aiRecommendedQuantity?.let { recAmt ->
                        if (recAmt.isNotBlank()) {
                            Button(
                                onClick = { Toast.makeText(context, "Applying AI Optimized Metric: $recAmt", Toast.LENGTH_SHORT).show() },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NutritionUiTokens.AccentColor)
                            ) {
                                Text(
                                    text = "Apply AI Recommended Volume ($recAmt)",
                                    color = NutritionUiTokens.DarkBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // READ-ONLY BADGE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE65100).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE65100).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Read-Only: You cannot log meals for this user.", color = Color(0xFFFFB74D), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
// --- VISUAL UI COMPONENT SUB-FACTORIES ---

@Composable
fun MacroBadgeItem(
    modifier: Modifier,
    label: String,
    value: String,
    registryKey: String,
    fallbackColor: Color
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Safe Adaptive Icon Mapping Handler
        when (val iconResult = NutritionIconRegistry.getMacroIcon(registryKey)) {
            is NutritionIcon.Vector -> Icon(iconResult.imageVector, contentDescription = null, tint = fallbackColor, modifier = Modifier.size(20.dp))
            is NutritionIcon.Resource -> Icon(painterResource(id = iconResult.resId), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(label, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun InsightRowItem(icon: ImageVector, color: Color, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CollapsiblePanelSection(title: String, content: String, isThreat: Boolean = false) {
    var panelExpanded by remember { mutableStateOf(false) }
    val interactiveTitleColor = if (isThreat) Color(0xFFFF7B7B) else Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { panelExpanded = !panelExpanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = interactiveTitleColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Icon(
                imageVector = if (panelExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color.DarkGray
            )
        }
        AnimatedVisibility(visible = panelExpanded) {
            Text(
                text = content,
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            )
        }
    }
}