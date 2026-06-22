package com.example.healthx.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.healthx.data.local.SavedAccount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    account: SavedAccount,
    onLogoutRequested: () -> Unit // Useful for testing switching accounts later
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HealthX", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212)),
                actions = {
                    IconButton(onClick = onLogoutRequested) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.Gray)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Profile Image Rendering
            if (!account.profilePhotoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = account.profilePhotoUrl,
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )
            } else {
                // Fallback to stylized initials if no photo exists
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2C2C)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = account.initials,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Welcome Text
            Text(
                text = "Hi, ${account.name}",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You are on your homepage.",
                color = Color.Gray,
                fontSize = 16.sp
            )

            // Helpful tag to see if you are in guest mode
            if (account.isGuest) {
                Spacer(modifier = Modifier.height(16.dp))
                SuggestionChip(
                    onClick = { },
                    label = { Text("Guest Mode Active", color = MaterialTheme.colorScheme.primary) }
                )
            }
        }
    }
}