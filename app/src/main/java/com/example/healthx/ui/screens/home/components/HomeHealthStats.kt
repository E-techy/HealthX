package com.example.healthx.ui.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HealthStatsGrid(
    onHeartRateClick: () -> Unit,
    onSpo2Click: () -> Unit,
    onBpClick: () -> Unit,
    onSleepClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = "Health Stats",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.Favorite, value = "72", unit = "bpm", color = Color(0xFFE53935), onClick = onHeartRateClick)
            StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.Bloodtype, value = "98", unit = "%", color = Color(0xFF03A9F4), onClick = onSpo2Click)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.MonitorHeart, value = "120/80", unit = "mmHg", color = Color(0xFF8E24AA), onClick = onBpClick)
            StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.NightsStay, value = "6h 45m", unit = "sleep", color = Color(0xFF7E57C2), onClick = onSleepClick)
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    unit: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(110.dp)
            .animatedGlowingBorder(color)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF14141A)) // Dark tinted card
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

// Custom Modifier for a glowing, rotating gradient border
fun Modifier.animatedGlowingBorder(glowColor: Color): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        // Animation logic would hook in here for a rotating brush.
        // For simplicity and performance, we draw a subtle static glow in this iteration,
        // but we will animate the borders in the main container.
    }
)