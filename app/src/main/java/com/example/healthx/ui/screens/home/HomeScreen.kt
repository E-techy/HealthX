package com.example.healthx.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthx.data.local.SavedAccount
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    account: SavedAccount,
    hasMultipleAccounts: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToApiKeys: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onSwitchAccountRequested: () -> Unit,
    onLogoutRequested: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val viewModel: HomeViewModel = viewModel()

    val subStatus by viewModel.subscriptionStatus.collectAsState()

    LaunchedEffect(account) {
        viewModel.fetchSubscriptionStatus(account)
    }

    // Wrap everything in the animated background
    AnimatedWaveBackground {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color(0xFF1E1E1E),
                    modifier = Modifier.width(300.dp)
                ) {
                    DrawerHeader(account = account, subStatus = subStatus)

                    Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        DrawerItem(icon = Icons.Default.Settings, label = "Settings") {
                            coroutineScope.launch { drawerState.close() }
                            onNavigateToSettings()
                        }
                        DrawerItem(icon = Icons.Default.VpnKey, label = "API Keys") {
                            coroutineScope.launch { drawerState.close() }
                            onNavigateToApiKeys()
                        }
                        DrawerItem(icon = Icons.Default.Chat, label = "AI Chat") {
                            coroutineScope.launch { drawerState.close() }
                            onNavigateToAiChat()
                        }
                        DrawerItem(icon = Icons.Default.NotificationsActive, label = "Reminders") {
                            coroutineScope.launch { drawerState.close() }
                            onNavigateToReminders()
                        }
                        DrawerItem(icon = Icons.Default.QrCodeScanner, label = "Scanner") {
                            coroutineScope.launch { drawerState.close() }
                            onNavigateToScanner()
                        }
                        DrawerItem(icon = Icons.Default.CardMembership, label = "Subscriptions") {
                            coroutineScope.launch { drawerState.close() }
                            onNavigateToSubscriptions()
                        }

                        if (hasMultipleAccounts) {
                            DrawerItem(icon = Icons.Default.SwitchAccount, label = "Switch Accounts") {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.switchAccount(onSuccess = onSwitchAccountRequested)
                            }
                        }
                    }

                    Divider(color = Color.DarkGray, thickness = 1.dp)
                    DrawerItem(
                        icon = Icons.Default.Logout,
                        label = "Log Out",
                        color = Color(0xFFE53935),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.logout(account.accountId, onSuccess = onLogoutRequested)
                    }
                }
            }
        ) {
            Scaffold(
                // Set to transparent so the animated wave shows through
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        // Changed to display the user's name
                        title = { Text(account.name, color = Color.White, fontWeight = FontWeight.Bold) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xBB121212)),
                        navigationIcon = {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                ProfileIcon(account = account, size = 32)
                            }
                        }
                    )
                },
                // Sticky center-bottom button for the Scanner
                floatingActionButtonPosition = FabPosition.Center,
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = onNavigateToScanner,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "QR Scanner", modifier = Modifier.size(32.dp))
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Welcome back, ${account.name}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap your profile icon to access the menu.",
                            color = Color.LightGray,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// --- ANIMATED BACKGROUND ---
@Composable
fun AnimatedWaveBackground(content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Cinematic dark/purple/blue cosmic wave
    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0A0A14),
            Color(0xFF141432),
            Color(0xFF1E0B2D),
            Color(0xFF0A0A14)
        ),
        start = Offset(offset, offset / 2),
        end = Offset(offset + 1000f, offset + 1000f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
    ) {
        content()
    }
}

@Composable
fun DrawerHeader(account: SavedAccount, subStatus: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        ProfileIcon(account = account, size = 64)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = account.name,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            shape = CircleShape,
            color = getStatusColor(subStatus).copy(alpha = 0.2f),
            contentColor = getStatusColor(subStatus)
        ) {
            Text(
                text = subStatus.uppercase(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProfileIcon(account: SavedAccount, size: Int) {
    if (!account.profilePhotoUrl.isNullOrBlank()) {
        coil.compose.SubcomposeAsyncImage(
            model = account.profilePhotoUrl,
            contentDescription = "Profile Picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape),
            loading = { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) },
            error = { InitialsFallback(account, size) }
        )
    } else {
        InitialsFallback(account, size)
    }
}

@Composable
fun InitialsFallback(account: SavedAccount, size: Int) {
    val colors = listOf(Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFF8E24AA), Color(0xFFF4511E), Color(0xFF00ACC1))
    val bgColor = colors[account.name.hashCode().absoluteValue % colors.size]

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = account.initials,
            color = Color.White,
            fontSize = (size / 2.5).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = label, tint = color) },
        label = { Text(label, color = color, fontSize = 16.sp, fontWeight = FontWeight.Medium) },
        selected = false,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent
        ),
        modifier = modifier.padding(vertical = 4.dp)
    )
}

fun getStatusColor(status: String): Color {
    return when (status.uppercase()) {
        "PRO" -> Color(0xFFFDD835)
        "ULTRA" -> Color(0xFFE040FB)
        "FREE" -> Color(0xFF9E9E9E)
        else -> Color(0xFF1E88E5)
    }
}