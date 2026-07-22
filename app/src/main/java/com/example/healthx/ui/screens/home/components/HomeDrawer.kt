package com.example.healthx.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.healthx.data.local.DelegatedSession
import com.example.healthx.data.local.SavedAccount
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun HomeDrawerContent(
    drawerState: DrawerState,
    account: SavedAccount,
    subStatus: String,
    hasMultipleAccounts: Boolean,
    delegatedSession: DelegatedSession?, // NEW PARAMETER
    onShowQrClicked: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToApiKeys: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToAlarmManager: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToNutrition: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToDelegatedAccess: () -> Unit,
    onSwitchAccountRequested: () -> Unit,
    onLogoutRequested: () -> Unit,
    onNavigateToDocsManager: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isGuest = delegatedSession != null

    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF141414),
        modifier = Modifier.width(320.dp)
    ) {
        DrawerHeader(
            profileUrl = delegatedSession?.profilePhotoUrl ?: account.profilePhotoUrl,
            name = delegatedSession?.name ?: account.name,
            subStatus = subStatus,
            isGuest = isGuest,
            onShowQrClicked = {
                coroutineScope.launch { drawerState.close() }
                onShowQrClicked()
            }
        )

        Divider(color = Color(0xFF2C2C2C), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
        ) {
            // PERMISSION CONTROLLED ITEMS
            if (!isGuest || delegatedSession!!.hasPermission("SEE_SETTINGS")) {
                DrawerItem(icon = Icons.Default.Settings, label = "Settings") {
                    coroutineScope.launch { drawerState.close() }; onNavigateToSettings()
                }
            }

            if (!isGuest || delegatedSession!!.hasPermission("SEE_REMINDERS")) {
                DrawerItem(icon = Icons.Default.NotificationsActive, label = "Reminders") {
                    coroutineScope.launch { drawerState.close() }; onNavigateToReminders()
                }
            }

            if (!isGuest || delegatedSession!!.hasPermission("SEE_NUTRITION")) {
                DrawerItem(icon = Icons.Default.Restaurant, label = "Nutrition") {
                    coroutineScope.launch { drawerState.close() }; onNavigateToNutrition()
                }
            }

            if (!isGuest || delegatedSession!!.hasPermission("SEE_SUBSCRIPTION")) {
                DrawerItem(icon = Icons.Default.CardMembership, label = "Subscriptions") {
                    coroutineScope.launch { drawerState.close() }; onNavigateToSubscriptions()
                }
            }

            if (!isGuest || delegatedSession!!.hasPermission("SEE_DOCS")) {
                DrawerItem(icon = Icons.Default.Folder, label = "Docs Manager") {
                    coroutineScope.launch { drawerState.close() }; onNavigateToDocsManager() // You need to pass this callback
                }
            }

            // DEVICE SPECIFIC ITEMS (Completely Hidden in Guest Mode)
            if (!isGuest) {
                DrawerItem(icon = Icons.Default.VpnKey, label = "API Keys") {
                    coroutineScope.launch { drawerState.close() }; onNavigateToApiKeys()
                }
                DrawerItem(icon = Icons.Default.Chat, label = "AI Chat") {
                    coroutineScope.launch { drawerState.close() }; onNavigateToAiChat()
                }
                DrawerItem(icon = Icons.Default.AccessAlarm, label = "Alarm Manager") {
                    coroutineScope.launch { drawerState.close() }; onNavigateToAlarmManager()
                }
                DrawerItem(icon = Icons.Default.QrCodeScanner, label = "Scanner") {
                    coroutineScope.launch { drawerState.close() }; onNavigateToScanner()
                }
                DrawerItem(icon = Icons.Default.Security, label = "Access Gateway") {
                    coroutineScope.launch { drawerState.close() }; onNavigateToDelegatedAccess()
                }
                DrawerItem(icon = Icons.Default.SwitchAccount, label = "Add / Switch Account") {
                    coroutineScope.launch { drawerState.close() }; onSwitchAccountRequested()
                }
            }
        }

        // Hide Logout in Guest Mode
        if (!isGuest) {
            Divider(color = Color(0xFF2C2C2C), thickness = 1.dp)
            DrawerItem(
                icon = Icons.Default.Logout,
                label = "Log Out",
                color = Color(0xFFE53935),
                modifier = Modifier.padding(16.dp)
            ) {
                coroutineScope.launch { drawerState.close() }; onLogoutRequested()
            }
        }
    }
}

@Composable
private fun DrawerHeader(
    profileUrl: String?,
    name: String,
    subStatus: String,
    isGuest: Boolean,
    onShowQrClicked: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            ProfileIcon(profileUrl = profileUrl, name = name, size = 64)

            // Hide the QR code share button if viewing a guest profile
            if (!isGuest) {
                IconButton(
                    onClick = onShowQrClicked,
                    modifier = Modifier.background(Color(0xFF222222), CircleShape).size(48.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = "Share Profile", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            color = if (isGuest) Color(0xFF00E676) else Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        val statusColor = when (subStatus.uppercase()) {
            "PRO" -> Color(0xFFFDD835)
            "ULTRA" -> Color(0xFFE040FB)
            "FREE" -> Color(0xFF9E9E9E)
            else -> Color(0xFF1E88E5)
        }

        Surface(
            shape = CircleShape,
            color = statusColor.copy(alpha = 0.15f),
            contentColor = statusColor
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
fun ProfileIcon(profileUrl: String?, name: String, size: Int) {
    if (!profileUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = profileUrl,
            contentDescription = "Profile Picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size.dp).clip(CircleShape),
            loading = { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) },
            error = { InitialsFallback(name, size) }
        )
    } else {
        InitialsFallback(name, size)
    }
}

@Composable
private fun InitialsFallback(name: String, size: Int) {
    val colors = listOf(Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFF8E24AA), Color(0xFFF4511E), Color(0xFF00ACC1))
    val safeName = if (name.isNotBlank()) name else "?"
    val bgColor = colors[safeName.hashCode().absoluteValue % colors.size]

    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = safeName.take(1).uppercase(),
            color = Color.White,
            fontSize = (size / 2.5).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DrawerItem(
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
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        modifier = modifier.padding(vertical = 4.dp)
    )
}