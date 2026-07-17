package com.example.healthx.nutrition_manager

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthx.data.model.NutritionGoal
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionGoalsScreen(viewModel: NutritionViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { com.example.healthx.data.local.SessionManager(context) }
    val delegatedSession by sessionManager.delegatedSessionFlow.collectAsState()

    val isGuest = delegatedSession != null
    val canEditGoals = !isGuest || delegatedSession!!.hasPermission("EDIT_GOALS")

    var showCreateSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchGoals()
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
                Text("Command Center: Goals", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
        },
        floatingActionButton = {
            // HIDE FAB IF THEY LACK EDIT PERMISSIONS
            if (canEditGoals) {
                FloatingActionButton(
                    onClick = { viewModel.navigateTo(NutritionScreenState.CreateGoal) },
                    containerColor = NutritionUiTokens.AccentColor,
                    contentColor = NutritionUiTokens.DarkBackground
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Goal")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {

            // Filter Row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf("Active", "Expired", "All")
                items(filters) { filter ->
                    FilterChipUI(
                        label = filter,
                        isSelected = viewModel.selectedGoalFilter.value == filter,
                        onClick = {
                            viewModel.selectedGoalFilter.value = filter
                            viewModel.fetchGoals()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (viewModel.isFetchingGoals.value) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NutritionUiTokens.AccentColor)
                }
            } else if (viewModel.goalsList.value.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No goals established. Initialize a new protocol.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxSize()) {
                    items(viewModel.goalsList.value) { goal ->
                        AdvancedGoalCard(goal)
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) } // FAB padding
                }
            }
        }
    }

    if (showCreateSheet) {
        // Implement bottom sheet for creation here later. For now, closes immediately.
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            containerColor = NutritionUiTokens.SurfaceDark
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Initialize Goal Matrix", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("UI Form for CreateGoalRequest goes here.", color = Color.Gray)
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun AdvancedGoalCard(goal: NutritionGoal) {
    // Extract today's progress, or use empty lists if no logs exist yet
    val todayString = SimpleDateFormat("yyyy-MM-dd", LocalLocale.current.platformLocale).format(Date())
    val todayProgress = goal.progressChart?.find { it.date == todayString }

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = NutritionUiTokens.SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Flag, contentDescription = null, tint = NutritionUiTokens.AccentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = goal.goalType.replace("_", " "),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }

                Box(
                    modifier = Modifier
                        .background(if (goal.isActive) Color(0xFF00E676).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(if (goal.isActive) "ACTIVE" else "TERMINATED", color = if (goal.isActive) Color(0xFF00E676) else Color.Red, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Lifecycle: ${goal.goalStartDate.take(10)} → ${goal.goalEndDate.take(10)}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(24.dp))

            Text("Today's Telemetry", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Radial Progress Grid
            // Maps exactly to the Targets defined by the user
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                goal.targets.forEach { target ->
                    // Find matching progress for this specific target
                    val matchingProgress = todayProgress?.nutrientProgress?.find { it.nutrientName == target.nutrientName }
                    val currentAmount = matchingProgress?.amountCompleted ?: "0"

                    // Parse "160g" -> 160f for math
                    val currentVal = extractNumber(currentAmount)
                    val targetVal = extractNumber(target.targetAmount)
                    val percentage = if (targetVal > 0) (currentVal / targetVal).coerceAtMost(1f) else 0f

                    // Dynamic Color based on Nutrient type
                    val ringColor = when (target.nutrientName.lowercase()) {
                        "protein" -> Color(0xFF4CAF50)
                        "calories" -> Color(0xFFFFA500)
                        "carbs" -> Color(0xFF29B6F6)
                        "fat" -> Color(0xFFFFEB3B)
                        else -> NutritionUiTokens.AccentColor
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        AnimatedCircularRing(
                            percentage = percentage,
                            color = ringColor,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(target.nutrientName, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text("$currentAmount / ${target.targetAmount}", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

// Utility to rip numbers out of strings like "1450 kcal" or "160g"
fun extractNumber(value: String): Float {
    val numericRegex = "[0-9]+(?:\\.[0-9]+)?".toRegex()
    val match = numericRegex.find(value)
    return match?.value?.toFloatOrNull() ?: 0f
}

@Composable
fun AnimatedCircularRing(percentage: Float, color: Color, modifier: Modifier = Modifier) {
    // Animates the sweep of the ring when the screen loads
    val animatedProgress by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 1500),
        label = "progress_animation"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()

            // Draw background track
            drawArc(
                color = color.copy(alpha = 0.15f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )

            // Draw animated progress
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Show percentage text in the center
        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}