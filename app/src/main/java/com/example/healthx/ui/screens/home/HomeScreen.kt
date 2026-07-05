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
    onSwitchAccountRequested: () -> Unit,
    onLogoutRequested: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val viewModel: HomeViewModel = viewModel()
    val subStatus by viewModel.subscriptionStatus.collectAsState()

    // 1. Initialize the Alarm Manager ViewModel to fetch active alarms for the dashboard
    val alarmViewModel: AlarmManagerViewModel = viewModel()
    val activeAlarms by alarmViewModel.activeAlarms.collectAsState()

    var showQrDialog by remember { mutableStateOf(false) }

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
                    onShowQrClicked = { showQrDialog = true },
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToApiKeys = onNavigateToApiKeys,
                    onNavigateToAiChat = onNavigateToAiChat,
                    onNavigateToReminders = onNavigateToReminders,
                    onNavigateToAlarmManager = onNavigateToAlarmManager,
                    onNavigateToScanner = onNavigateToScanner,
                    onNavigateToSubscriptions = onNavigateToSubscriptions,
                    onSwitchAccountRequested = onSwitchAccountRequested,
                    onLogoutRequested = onLogoutRequested
                )
            }
        ) {
            Scaffold(
                containerColor = Color.Transparent, // Let the cosmic wave shine through
                topBar = {
                    TopAppBar(
                        title = {}, // Empty title, greeting handles it
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        navigationIcon = {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                ProfileIcon(account = account, size = 42)
                            }
                        }
                    )
                },
                floatingActionButtonPosition = FabPosition.Center,
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = onNavigateToScanner,
                        containerColor = Color(0xFF1E88E5), // Vivid Blue
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "QR Scanner", modifier = Modifier.size(32.dp))
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 100.dp) // Space for the FAB
                ) {
                    // --- GREETING ---
                    item {
                        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                            Text(
                                text = "Welcome back,",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                            Text(
                                text = account.name,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // --- 1. HEALTH STATS GRID ---
                    item {
                        HealthStatsGrid(
                            onHeartRateClick = { /* TODO: Route */ },
                            onSpo2Click = { /* TODO: Route */ },
                            onBpClick = { /* TODO: Route */ },
                            onSleepClick = { /* TODO: Route */ }
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // --- 2. NUTRITION GOALS ---
                    item {
                        HomeNutritionSection(
                            onNutritionClick = { /* TODO: Navigate to full nutrition screen */ }
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // --- 3. UPCOMING ALARMS ---
                    item {
                        HomeUpcomingAlarms(
                            activeAlarms = activeAlarms,
                            onOpenAlarmManager = onNavigateToAlarmManager
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // --- 4. MEETINGS & CONSULTATIONS ---
                    item {
                        HomeMeetings(
                            onMeetingsClick = { /* TODO: Navigate to video calls screen */ }
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    if (showQrDialog) {
        ShareProfileFullScreenDialog(account = account, onClose = { showQrDialog = false })
    }
}

// --- FULL SCREEN QR DIALOG ---
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
        // Generate the QR code
        qrBitmap = QRGenerator.generateQRCode(data = payload, size = 600)
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // Makes the dialog full-screen
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

// Background Animator for the Cosmic Cinematic Feel
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
            Color(0xFF0A0A12), // Deep Space
            Color(0xFF1A1A2E), // Dark Purple tint
            Color(0xFF0F1A24), // Dark Blue tint
            Color(0xFF0A0A12)
        ),
        start = Offset(offset, offset / 2),
        end = Offset(offset + 1000f, offset + 1500f)
    )

    Box(modifier = Modifier.fillMaxSize().background(brush)) { content() }
}