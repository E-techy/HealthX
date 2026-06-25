package com.example.healthx.ui.screens.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthx.utils.LocalActiveAccount

data class CategoryMeta(val id: String, val title: String, val icon: ImageVector, val color: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersHomeScreen(onCategoryClick: (String) -> Unit) {
    val user = LocalActiveAccount.current

    val categories = listOf(
        CategoryMeta("pharmacy", "Pharmacy & Pills", Icons.Default.Medication, Color(0xFFE53935)),
        CategoryMeta("visits", "Medical Visits", Icons.Default.LocalHospital, Color(0xFF1E88E5)),
        CategoryMeta("wellness", "Daily Wellness", Icons.Default.SelfImprovement, Color(0xFF43A047)),
        CategoryMeta("tracking", "Health Tracking", Icons.Default.MonitorHeart, Color(0xFF8E24AA)),
        CategoryMeta("care", "Specialized Care", Icons.Default.Elderly, Color(0xFFFDD835)),
        CategoryMeta("custom", "Custom", Icons.Default.Star, Color(0xFF00ACC1))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // --- TOP: Upcoming Reminders (Horizontal Scroll) ---
            Text(
                text = "Upcoming for ${user?.name}",
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(3) { // Dummy data loop for now
                    UpcomingReminderCard()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTTOM: Categories Grid ---
            Text(
                text = "Categories",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(categories) { category ->
                    CategoryCard(category) { onCategoryClick(category.id) }
                }
            }
        }
    }
}

@Composable
fun UpcomingReminderCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.width(200.dp).height(100.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text("Paracetamol 500mg", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("In 45 mins • After Meal", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun CategoryCard(category: CategoryMeta, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.height(120.dp).clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(category.color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(category.icon, contentDescription = null, tint = category.color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(category.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}