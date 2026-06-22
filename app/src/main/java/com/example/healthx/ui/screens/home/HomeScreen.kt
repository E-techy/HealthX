package com.example.healthx.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.SwitchAccount
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
    hasMultipleAccounts: Boolean,
    onLogoutRequested: () -> Unit,
    onSwitchAccountRequested: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HealthX", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212)),
                actions = {
                    if (hasMultipleAccounts) {
                        IconButton(onClick = onSwitchAccountRequested) {
                            Icon(Icons.Default.SwitchAccount, contentDescription = "Switch Account", tint = Color.Gray)
                        }
                    }
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
                coil.compose.SubcomposeAsyncImage(
                    model = account.profilePhotoUrl,
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    loading = {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    error = {
                        // Safe fallback if the server is offline or URL is broken
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = account.initials, color = MaterialTheme.colorScheme.primary, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            } else {
                // Fallback to stylized initials if no photo URL exists at all
                Box(
                    modifier = Modifier.size(120.dp).clip(CircleShape).background(Color(0xFF2C2C2C)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = account.initials, color = MaterialTheme.colorScheme.primary, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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

            if (account.isGuest) {
                Spacer(modifier = Modifier.height(16.dp))
                SuggestionChip(
                    onClick = { },
                    label = { Text("Guest Mode Active", color = MaterialTheme.colorScheme.primary) }
                )
            }

            if (hasMultipleAccounts) {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(onClick = onSwitchAccountRequested) {
                    Text("Switch Account")
                }
            }
        }
    }
}