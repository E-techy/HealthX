package com.example.healthx.ui.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

@Composable
fun HomeNutritionSection(
    onNutritionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onNutritionClick() }
    ) {
        Text(
            text = "Daily Nutrition & Goals",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Calories: 65% Full (Orange)
            LiquidStatCard(
                modifier = Modifier.weight(1f),
                title = "Calories",
                value = "1,560",
                target = "/ 2400 kcal",
                percentage = 0.65f,
                icon = Icons.Default.Whatshot,
                liquidColor = Color(0xFFFF9800),
                bgColor = Color(0xFF3E2723)
            )

            // Water: 45% Full (Blue)
            LiquidStatCard(
                modifier = Modifier.weight(1f),
                title = "Water",
                value = "1.8",
                target = "/ 4.0 L",
                percentage = 0.45f,
                icon = Icons.Default.LocalDrink,
                liquidColor = Color(0xFF2196F3),
                bgColor = Color(0xFF0D47A1)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Protein: 80% Full (Green)
        LiquidStatCard(
            modifier = Modifier.fillMaxWidth().height(100.dp), // Wider card for protein
            title = "Protein Intake",
            value = "112g",
            target = "/ 140g",
            percentage = 0.80f,
            icon = Icons.Default.Restaurant,
            liquidColor = Color(0xFF4CAF50),
            bgColor = Color(0xFF1B5E20)
        )
    }
}

@Composable
fun LiquidStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    target: String,
    percentage: Float,
    icon: ImageVector,
    liquidColor: Color,
    bgColor: Color
) {
    // 1. Setup the infinite wave animation
    val infiniteTransition = rememberInfiniteTransition()
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1A1A1A)) // Deep base card color
    ) {
        // 2. Draw the Animated Liquid Background
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate where the water level should be (0% is bottom, 100% is top)
            val waterLevel = canvasHeight * (1f - percentage)
            val waveAmplitude = 12f // Height of the waves

            val path = Path().apply {
                moveTo(0f, canvasHeight) // Start at bottom left
                lineTo(0f, waterLevel)   // Go up to water level

                // Draw the sine wave across the width
                for (x in 0..canvasWidth.toInt() step 5) {
                    val y = waterLevel + sin((x / 50f) + waveOffset) * waveAmplitude
                    lineTo(x.toFloat(), y)
                }

                lineTo(canvasWidth, canvasHeight) // Down to bottom right
                close() // Back to start
            }

            // Fill the wave with a beautiful gradient
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(liquidColor.copy(alpha = 0.8f), bgColor.copy(alpha = 0.4f)),
                    startY = waterLevel,
                    endY = canvasHeight
                )
            )
        }

        // 3. Draw the Text and Icons over the liquid
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = liquidColor, modifier = Modifier.size(28.dp))
                Text(text = "${(percentage * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Column {
                Text(text = title, color = Color.LightGray, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = target, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
}