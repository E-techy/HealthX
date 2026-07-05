package com.example.healthx.ui.screens.home.components
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoCall
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

@Composable
fun HomeMeetings(
    onMeetingsClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = "Scheduled Consultations",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14141A)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().clickable { onMeetingsClick() }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF0D47A1)), // Blue background
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.VideoCall, contentDescription = "Video Call", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text("Dr. Sarah Jenkins", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("General Checkup • Video", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Today, 4:30 PM", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}