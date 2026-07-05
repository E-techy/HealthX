package com.example.healthx.ui.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
fun HomeNutritionSection(onNutritionClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Nutrition & Goals", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.clickable { onNutritionClick() }, verticalAlignment = Alignment.CenterVertically) {
                Text("Details", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
            }
        }

        // Mini Health Score Summary
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF14141A)).clickable { onNutritionClick() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Health Score", color = Color.Gray, fontSize = 14.sp)
                Text("Excellent (91%)", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF4CAF50).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Text("91", color = Color(0xFF4CAF50), fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LiquidStatCard(modifier = Modifier.weight(1f), title = "Calories", value = "1,560", target = "/ 2400 kcal", percentage = 0.65f, icon = Icons.Default.Whatshot, liquidColor = Color(0xFFFF9800), bgColor = Color(0xFF3E2723))
            LiquidStatCard(modifier = Modifier.weight(1f), title = "Water", value = "1.8", target = "/ 4.0 L", percentage = 0.45f, icon = Icons.Default.LocalDrink, liquidColor = Color(0xFF2196F3), bgColor = Color(0xFF0D47A1))
        }

        Spacer(modifier = Modifier.height(16.dp))

        LiquidStatCard(modifier = Modifier.fillMaxWidth().height(90.dp), title = "Protein Intake", value = "112g", target = "/ 140g", percentage = 0.80f, icon = Icons.Default.FitnessCenter, liquidColor = Color(0xFF4CAF50), bgColor = Color(0xFF1B5E20))
    }
}

// ... [Keep the LiquidStatCard function you already provided here exactly as is] ...

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
            .background(Color(0xFF1A1A1A))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val waterLevel = canvasHeight * (1f - percentage)
            val waveAmplitude = 12f

            val path = Path().apply {
                moveTo(0f, canvasHeight)
                lineTo(0f, waterLevel)

                for (x in 0..canvasWidth.toInt() step 5) {
                    val y = waterLevel + sin((x / 50f) + waveOffset) * waveAmplitude
                    lineTo(x.toFloat(), y)
                }

                lineTo(canvasWidth, canvasHeight)
                close()
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(liquidColor.copy(alpha = 0.8f), bgColor.copy(alpha = 0.4f)),
                    startY = waterLevel,
                    endY = canvasHeight
                )
            )
        }

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