package com.example.healthx.ui.screens.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SubCategoryItem(val route: String, val title: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubCategoryScreen(
    categoryId: String,
    onBack: () -> Unit,
    onNavigateToForm: (String) -> Unit
) {
    // Dynamically load the correct sub-items based on the Domain clicked on the Home Screen
    val (title, subItems) = when (categoryId) {
        "pharmacy" -> "Pharmacy & Pills" to listOf(
            SubCategoryItem("form_medication", "Medications", Icons.Default.Medication),
            SubCategoryItem("form_supplement", "Vitamins & Supplements", Icons.Default.LocalPharmacy),
            SubCategoryItem("form_refill", "Pharmacy Refills", Icons.Default.Store)
        )
        "visits" -> "Medical Visits" to listOf(
            SubCategoryItem("form_consultation", "Doctor Consultations", Icons.Default.PersonSearch),
            SubCategoryItem("form_checkup", "Routine Checkups", Icons.Default.HealthAndSafety),
            SubCategoryItem("form_lab", "Lab Diagnostics", Icons.Default.Science),
            SubCategoryItem("form_therapy", "Therapy Sessions", Icons.Default.Psychology),
            SubCategoryItem("form_vaccine", "Immunization", Icons.Default.Vaccines)
        )
        "wellness" -> "Daily Wellness" to listOf(
            SubCategoryItem("form_hydration", "Hydration", Icons.Default.WaterDrop),
            SubCategoryItem("form_nutrition", "Diet & Nutrition", Icons.Default.Restaurant),
            SubCategoryItem("form_sleep", "Sleep Routine", Icons.Default.Bedtime),
            SubCategoryItem("form_fitness", "Fitness & Activity", Icons.Default.FitnessCenter),
            SubCategoryItem("form_mindfulness", "Mindfulness", Icons.Default.SelfImprovement),
            SubCategoryItem("form_habit", "Habit Builder", Icons.Default.Loop)
        )
        "tracking" -> "Health Tracking" to listOf(
            SubCategoryItem("form_vitals", "Vitals & Biometrics", Icons.Default.MonitorHeart),
            SubCategoryItem("form_symptom", "Symptom Log", Icons.Default.Sick),
            SubCategoryItem("form_cycle", "Cycle Tracking", Icons.Default.CalendarMonth)
        )
        "care" -> "Specialized Care" to listOf(
            SubCategoryItem("form_maternity", "Maternity Care", Icons.Default.PregnantWoman),
            SubCategoryItem("form_elder", "Elder Care Duty", Icons.Default.Elderly),
            SubCategoryItem("form_recovery", "Post-Op Recovery", Icons.Default.Healing)
        )
        else -> "Custom Reminders" to listOf(
            SubCategoryItem("form_custom", "Create Custom Reminder", Icons.Default.Star)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, tint = Color.White, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color.Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Select Reminder Type", color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(subItems) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    modifier = Modifier.fillMaxWidth().height(80.dp).clickable { onNavigateToForm(item.route) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2C2C2C)), contentAlignment = Alignment.Center) {
                            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(item.title, color = Color.White, fontSize = 18.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }
        }
    }
}