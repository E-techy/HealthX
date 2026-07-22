package com.example.healthx.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthx.alarm_manager.AlarmManagerViewModel
import com.example.healthx.data.local.SavedAccount
import com.example.healthx.qr_codes.ProfileQRBuilder
import com.example.healthx.ui.screens.home.components.*
import com.example.healthx.utils.QRGenerator
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.example.healthx.data.local.SessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    account: SavedAccount,
    hasMultipleAccounts: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToApiKeys: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToAlarmManager: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToNutrition: () -> Unit,
    onNavigateToDelegatedAccess: () -> Unit,
    onSwitchAccountRequested: () -> Unit,
    onLogoutRequested: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val delegatedSession by sessionManager.delegatedSessionFlow.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val viewModel: HomeViewModel = viewModel()
    val subStatus by viewModel.subscriptionStatus.collectAsState()

    val alarmViewModel: AlarmManagerViewModel = viewModel()
    val activeAlarms by alarmViewModel.activeAlarms.collectAsState()

    var showQrDialog by remember { mutableStateOf(false) }

    // PERMISSION CHECKS (Defaults to true if NOT in guest mode)
    val isGuest = delegatedSession != null
    val canSeeNutrition = !isGuest || delegatedSession!!.hasPermission("SEE_NUTRITION")
    val canSeeReminders = !isGuest || delegatedSession!!.hasPermission("SEE_REMINDERS")

    LaunchedEffect(account) { viewModel.fetchSubscriptionStatus(account) }

    AnimatedWaveBackground {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                HomeDrawerContent(
                    drawerState = drawerState,
                    account = account,
                    subStatus = subStatus,
                    hasMultipleAccounts = hasMultipleAccounts,
                    delegatedSession = delegatedSession, // Pass session to hide drawer items
                    onShowQrClicked = { showQrDialog = true },
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToApiKeys = onNavigateToApiKeys,
                    onNavigateToAiChat = onNavigateToAiChat,
                    onNavigateToReminders = onNavigateToReminders,
                    onNavigateToAlarmManager = onNavigateToAlarmManager,
                    onNavigateToScanner = onNavigateToScanner,
                    onNavigateToNutrition = onNavigateToNutrition,
                    onNavigateToSubscriptions = onNavigateToSubscriptions,
                    onNavigateToDelegatedAccess = onNavigateToDelegatedAccess,
                    onSwitchAccountRequested = { viewModel.switchAccount { onSwitchAccountRequested() } },
                ) { viewModel.logout(account.accountId) { onLogoutRequested() } }
            }
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {},
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        navigationIcon = {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                ProfileIcon(
                                    profileUrl = delegatedSession?.profilePhotoUrl ?: account.profilePhotoUrl,
                                    name = delegatedSession?.name ?: account.name,
                                    size = 42
                                )
                            }
                        }
                    )
                },
                floatingActionButtonPosition = FabPosition.Center,
                floatingActionButton = {
                    // Hide Scanner FAB entirely in Guest Mode
                    if (!isGuest) {
                        FloatingActionButton(
                            onClick = onNavigateToScanner,
                            containerColor = Color(0xFF1E88E5),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "QR Scanner", modifier = Modifier.size(32.dp))
                        }
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {

                    // 1. THE DELEGATED GUEST BANNER
                    if (isGuest) {
                        item {
                            DelegatedModeBanner(
                                targetName = delegatedSession!!.name,
                                onExit = { sessionManager.exitDelegatedMode() }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // 2. THE DYNAMIC GREETING
                    item {
                        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                            Text(
                                text = if (isGuest) "Viewing data for," else "Welcome back,",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                            Text(
                                text = delegatedSession?.name ?: account.name,
                                color = if (isGuest) Color(0xFF00E676) else Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // 3. STATS GRID (Always visible for now, or you can wrap in a permission)
                    item {
                        HealthStatsGrid(
                            onHeartRateClick = { /* TODO */ },
                            onSpo2Click = { /* TODO */ },
                            onBpClick = { /* TODO */ },
                            onSleepClick = { /* TODO */ }
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // 4. NUTRITION SECTION (Protected)
                    if (canSeeNutrition) {
                        item {
                            HomeNutritionSection(onNutritionClick = onNavigateToNutrition)
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }

                    // 5. ALARMS / REMINDERS SECTION (Protected)
                    if (canSeeReminders) {
                        item {
                            HomeUpcomingAlarms(
                                activeAlarms = activeAlarms,
                                onOpenAlarmManager = onNavigateToAlarmManager
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }

                    // 6. MEETINGS
                    item {
                        HomeMeetings(onMeetingsClick = { /* TODO */ })
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    if (showQrDialog && !isGuest) {
        ShareProfileFullScreenDialog(account = account, onClose = { showQrDialog = false })
    }
}

// ... Keep existing ShareProfileFullScreenDialog, AnimatedWaveBackground, DelegatedModeBanner ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareProfileFullScreenDialog(account: SavedAccount, onClose: () -> Unit) {
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(account) {
        val payload = ProfileQRBuilder.buildShareProfilePayload(
            accountId = account.accountId,
            name = account.name,
            email = account.email
        )
        qrBitmap = QRGenerator.generateQRCode(data = payload, size = 600)
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Share Profile", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
                )
            },
            containerColor = Color.Black
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Scan to Connect",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "Profile QR Code",
                            modifier = Modifier
                                .size(250.dp)
                                .padding(16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(250.dp)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text("Or add manually using User ID:", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = account.accountId,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedWaveBackground(content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0A0A12),
            Color(0xFF1A1A2E),
            Color(0xFF0F1A24),
            Color(0xFF0A0A12)
        ),
        start = Offset(offset, offset / 2),
        end = Offset(offset + 1000f, offset + 1500f)
    )

    Box(modifier = Modifier.fillMaxSize().background(brush)) { content() }
}

@Composable
fun DelegatedModeBanner(targetName: String, onExit: () -> Unit) {
    Surface(
        color = Color(0xFFE53935).copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Delegated Access Active", color = Color(0xFFE53935), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("You are viewing $targetName's data.", color = Color.White, fontSize = 12.sp)
            }
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Exit", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}