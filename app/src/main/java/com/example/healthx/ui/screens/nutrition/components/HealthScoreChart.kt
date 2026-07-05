package com.example.healthx.ui.screens.nutrition.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthx.ui.screens.nutrition.DailyHealthRecord

@Composable
fun HealthScoreChart(records: List<DailyHealthRecord>) {
    val maxScore = 100f
    val lineColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF14141A))
            .padding(24.dp)
    ) {
        Text("7-Day Health Trend", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val spacing = width / (records.size - 1)

                val points = records.mapIndexed { index, record ->
                    val x = index * spacing
                    val y = height - ((record.score / maxScore) * height)
                    Pair(x, y)
                }

                val path = Path().apply {
                    moveTo(points.first().first, points.first().second)
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val controlX1 = (p1.first + p2.first) / 2f
                        val controlX2 = (p1.first + p2.first) / 2f
                        cubicTo(controlX1, p1.second, controlX2, p2.second, p2.first, p2.second)
                    }
                }

                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.3f), Color.Transparent))
                )

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 4.dp.toPx())
                )

                points.forEach { point ->
                    drawCircle(color = Color.White, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(point.first, point.second))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            records.forEach { record ->
                Text(text = record.dayLabel, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}