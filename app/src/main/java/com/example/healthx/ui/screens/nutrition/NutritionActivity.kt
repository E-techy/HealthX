package com.example.healthx.ui.screens.nutrition

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthx.ui.screens.home.components.LiquidStatCard
import com.example.healthx.ui.screens.nutrition.components.HealthScoreChart
import com.example.healthx.ui.theme.HealthXTheme

class NutritionActivity : ComponentActivity() {
    private val viewModel: NutritionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthXTheme {
                NutritionDetailScreen(viewModel = viewModel, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionDetailScreen(viewModel: NutritionViewModel, onBack: () -> Unit) {
    val weeklyScores by viewModel.weeklyScores.collectAsState()
    val score = viewModel.todaysScore
    val scoreColor = viewModel.getScoreColor(score)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition & Health", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, tint = Color.White, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color.Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // --- HEADER: HEALTH SCORE ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF14141A)).padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Today's Health Score", color = Color.Gray, fontSize = 14.sp)
                        Text(viewModel.getScoreEvaluation(score), color = scoreColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(scoreColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Text("$score", color = scoreColor, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // --- GRAPH ---
            item { HealthScoreChart(records = weeklyScores) }

            // --- MACROS ---
            item {
                Text("Macronutrients", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LiquidStatCard(modifier = Modifier.weight(1f), title = "Calories", value = "1560", target = "/ 2400 kcal", percentage = 0.65f, icon = Icons.Default.Whatshot, liquidColor = Color(0xFFFF9800), bgColor = Color(0xFF3E2723))
                    LiquidStatCard(modifier = Modifier.weight(1f), title = "Protein", value = "112", target = "/ 140 g", percentage = 0.80f, icon = Icons.Default.FitnessCenter, liquidColor = Color(0xFF4CAF50), bgColor = Color(0xFF1B5E20))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LiquidStatCard(modifier = Modifier.weight(1f), title = "Carbs", value = "180", target = "/ 250 g", percentage = 0.72f, icon = Icons.Default.BakeryDining, liquidColor = Color(0xFF9C27B0), bgColor = Color(0xFF4A148C))
                    LiquidStatCard(modifier = Modifier.weight(1f), title = "Fat", value = "55", target = "/ 70 g", percentage = 0.78f, icon = Icons.Default.Opacity, liquidColor = Color(0xFFFFC107), bgColor = Color(0xFFE65100))
                }
            }

            // --- HYDRATION ---
            item {
                Text("Hydration & Electrolytes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                LiquidStatCard(modifier = Modifier.fillMaxWidth(), title = "Water Intake", value = "1.8", target = "/ 4.0 L", percentage = 0.45f, icon = Icons.Default.LocalDrink, liquidColor = Color(0xFF2196F3), bgColor = Color(0xFF0D47A1))
            }

            // --- MICRONUTRIENTS GRID ---
            item {
                Text("Micronutrients", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MicroCard("Fiber", "20 / 30 g", Icons.Default.Eco, Color(0xFF8BC34A))
                    MicroCard("Sugar", "45 / <50 g", Icons.Default.Icecream, Color(0xFFE91E63))
                    MicroCard("Sodium", "1500 / 2300 mg", Icons.Default.Science, Color(0xFF00BCD4))
                    MicroCard("Vitamin C", "80 / 90 mg", Icons.Default.WbSunny, Color(0xFFFFEB3B))
                }
            }
        }
    }
}

@Composable
fun MicroCard(title: String, value: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1E1E)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Text(value, color = Color.LightGray, fontSize = 14.sp)
    }
}