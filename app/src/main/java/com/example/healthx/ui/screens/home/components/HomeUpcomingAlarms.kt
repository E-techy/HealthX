package com.example.healthx.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthx.data.local.entities.AlarmEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeUpcomingAlarms(
    activeAlarms: List<AlarmEntity>,
    onOpenAlarmManager: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upcoming Alarms",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onOpenAlarmManager() }
            ) {
                Text("Manage", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
            }
        }

        if (activeAlarms.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                modifier = Modifier.fillMaxWidth().height(80.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active alarms.", color = Color.Gray)
                }
            }
        } else {
            // Show only the next 2 alarms to save space on Home Screen
            activeAlarms.take(2).forEach { alarm ->
                HomeAlarmItem(alarm = alarm)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun HomeAlarmItem(alarm: AlarmEntity) {
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val timeString = formatter.format(Date(alarm.triggerTimeMillis))
    val isCurrentlyRunning = alarm.status == "PENDING" && (alarm.triggerTimeMillis - System.currentTimeMillis() < 60_000)

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isCurrentlyRunning) Color(0xFF3E1E1E) else Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isCurrentlyRunning) Color(0xFFE53935) else Color(0xFF2C2C2C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrentlyRunning) Icons.Default.Warning else Icons.Default.AccessAlarm,
                    contentDescription = "Alarm",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = alarm.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = alarm.category, color = Color.LightGray, fontSize = 12.sp)
            }

            Text(
                text = timeString,
                color = if (isCurrentlyRunning) Color.White else MaterialTheme.colorScheme.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}