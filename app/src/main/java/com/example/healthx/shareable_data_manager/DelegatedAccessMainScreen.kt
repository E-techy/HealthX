package com.example.healthx.shareable_data_manager

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.healthx.data.local.SavedAccount
import com.example.healthx.shareable_data_manager.data.AccessPermissions
import com.example.healthx.shareable_data_manager.data.BlocklistedUser
import com.example.healthx.shareable_data_manager.data.ShareableHash
import com.example.healthx.utils.QRGenerator
import com.google.gson.Gson
import kotlinx.coroutines.launch

enum class CurrentScreen {
    QR_GENERATOR,
    ACTIVE_LINKS,
    BLOCKLIST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelegatedAccessMainScreen(
    account: SavedAccount,
    onBack: () -> Unit
) {
    val viewModel: DelegatedAccessViewModel = viewModel()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentScreen by remember { mutableStateOf(CurrentScreen.QR_GENERATOR) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF141414),
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Data Sharing",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                )
                Divider(color = Color(0xFF2C2C2C))

                DrawerMenuButton(
                    icon = Icons.Outlined.QrCode,
                    label = "My QR Code",
                    isSelected = currentScreen == CurrentScreen.QR_GENERATOR
                ) {
                    currentScreen = CurrentScreen.QR_GENERATOR
                    scope.launch { drawerState.close() }
                }

                DrawerMenuButton(
                    icon = Icons.Outlined.Link,
                    label = "Active Shared Links",
                    isSelected = currentScreen == CurrentScreen.ACTIVE_LINKS
                ) {
                    currentScreen = CurrentScreen.ACTIVE_LINKS
                    viewModel.fetchMyHashes(account.token)
                    scope.launch { drawerState.close() }
                }

                DrawerMenuButton(
                    icon = Icons.Outlined.Block,
                    label = "Blocked Users",
                    isSelected = currentScreen == CurrentScreen.BLOCKLIST
                ) {
                    currentScreen = CurrentScreen.BLOCKLIST
                    viewModel.fetchBlocklist(account.token)
                    scope.launch { drawerState.close() }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when(currentScreen) {
                                CurrentScreen.QR_GENERATOR -> "Share Data"
                                CurrentScreen.ACTIVE_LINKS -> "Manage Links"
                                CurrentScreen.BLOCKLIST -> "Blocked Users"
                            },
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                    },
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1A24))
                )
            },
            containerColor = Color(0xFF0A0A12)
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    CurrentScreen.QR_GENERATOR -> QRCodeGeneratorScreen(account, viewModel, snackbarHostState)
                    CurrentScreen.ACTIVE_LINKS -> ActiveLinksScreen(account.token, viewModel)
                    CurrentScreen.BLOCKLIST -> BlocklistScreen(account.token, viewModel)
                }
            }
        }
    }
}

@Composable
fun DrawerMenuButton(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = label, tint = if (isSelected) Color(0xFF1E88E5) else Color.White) },
        label = { Text(label, color = if (isSelected) Color(0xFF1E88E5) else Color.White, fontSize = 16.sp) },
        selected = isSelected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            selectedContainerColor = Color(0xFF1E88E5).copy(alpha = 0.1f)
        ),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

// ==========================================
// 1. QR GENERATOR SCREEN
// ==========================================
@Composable
fun QRCodeGeneratorScreen(
    account: SavedAccount,
    viewModel: DelegatedAccessViewModel,
    snackbarHostState: SnackbarHostState
) {
    val generateState by viewModel.generateHashState.collectAsState()

    var activeQrPayload by remember { mutableStateOf(
        Gson().toJson(mapOf("category" to "ADD_FRIEND", "userId" to account.accountId))
    )}
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPermissionsSheet by remember { mutableStateOf(false) }
    var isCustomQrActive by remember { mutableStateOf(false) }

    LaunchedEffect(activeQrPayload) {
        qrBitmap = QRGenerator.generateQRCode(data = activeQrPayload, size = 600)
    }

    LaunchedEffect(generateState) {
        if (generateState is UiState.Error) {
            snackbarHostState.showSnackbar((generateState as UiState.Error).message)
            viewModel.resetGenerateState()
        } else if (generateState is UiState.Success) {
            val hashData = (generateState as UiState.Success).data
            activeQrPayload = Gson().toJson(mapOf("category" to "SHARE_ACCESS", "hash" to hashData.hashId))
            isCustomQrActive = true
            showPermissionsSheet = false
            viewModel.resetGenerateState()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isCustomQrActive) "Custom Share Link Ready" else "Your Default QR Code",
            style = MaterialTheme.typography.titleMedium,
            color = if (isCustomQrActive) Color(0xFF00E676) else Color.LightGray
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap!!.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(260.dp).padding(16.dp)
                )
            } else {
                Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        if (isCustomQrActive) {
            Button(
                onClick = {
                    activeQrPayload = Gson().toJson(mapOf("category" to "ADD_FRIEND", "userId" to account.accountId))
                    isCustomQrActive = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.9f).height(56.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset to Default QR", fontSize = 16.sp, color = Color.White)
            }
        } else {
            Button(
                onClick = { showPermissionsSheet = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.9f).height(56.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Custom QR Code", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showPermissionsSheet) {
        PermissionsBottomSheet(
            onDismiss = { showPermissionsSheet = false },
            onGenerateClicked = { selectedPermissions -> viewModel.generateHash(account.token, selectedPermissions) },
            isLoading = generateState is UiState.Loading
        )
    }
}

// ==========================================
// 2. ACTIVE LINKS SCREEN (HASHES)
// ==========================================
@Composable
fun ActiveLinksScreen(token: String, viewModel: DelegatedAccessViewModel) {
    val hashesState by viewModel.myHashesState.collectAsState()

    when (val state = hashesState) {
        is UiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is UiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message, color = Color.Red) }
        is UiState.Success -> {
            val hashes = state.data
            if (hashes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active shared links found.", color = Color.Gray)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(hashes) { hash ->
                        HashCard(
                            hash = hash,
                            onToggleStatus = { viewModel.toggleHashStatus(token, hash.hashId, hash.status) },
                            onDelete = { viewModel.deleteHash(token, hash.hashId) }
                        )
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
fun HashCard(hash: ShareableHash, onToggleStatus: () -> Unit, onDelete: () -> Unit) {
    val isActive = hash.status == "ACTIVE"
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ID: ${hash.hashId.take(8)}...",
                    color = Color.White, fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFE53935))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text("Permissions granted:", color = Color.Gray, fontSize = 12.sp)
            hash.actions.forEach { action ->
                Text("• $action", color = Color(0xFF64B5F6), fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFF2C2C2C))
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(if (isActive) "Link is Active" else "Link is Paused", color = if (isActive) Color(0xFF00E676) else Color.Gray)
                Switch(
                    checked = isActive,
                    onCheckedChange = { onToggleStatus() },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF1E88E5))
                )
            }
        }
    }
}

// ==========================================
// 3. BLOCKLIST SCREEN
// ==========================================
@Composable
fun BlocklistScreen(token: String, viewModel: DelegatedAccessViewModel) {
    val blocklistState by viewModel.blocklistState.collectAsState()

    when (val state = blocklistState) {
        is UiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is UiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message, color = Color.Red) }
        is UiState.Success -> {
            val users = state.data
            if (users.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No blocked users.", color = Color.Gray)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(users) { user ->
                        BlocklistedUserRow(
                            user = user,
                            onUnblock = { viewModel.unblockUser(token, user.userId) }
                        )
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
fun BlocklistedUserRow(user: BlocklistedUser, onUnblock: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(Color(0xFF1A1A2E), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2C2C2C)),
                contentAlignment = Alignment.Center
            ) {
                if (user.profileImageUri != null) {
                    AsyncImage(model = user.profileImageUri, contentDescription = "Profile", modifier = Modifier.fillMaxSize())
                } else {
                    Text(user.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(user.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        TextButton(onClick = onUnblock) {
            Text("Unblock", color = Color(0xFF1E88E5), fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// BOTTOM SHEET (Unchanged functionality, updated styling)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsBottomSheet(
    onDismiss: () -> Unit,
    onGenerateClicked: (List<String>) -> Unit,
    isLoading: Boolean
) {
    val selectedPermissions = remember { mutableStateMapOf<String, Boolean>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                text = "Select Information to Share",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(AccessPermissions.availablePermissions) { permission ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(text = permission.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = permission.description, color = Color.LightGray, fontSize = 12.sp)
                        }
                        Switch(
                            checked = selectedPermissions[permission.id] ?: false,
                            onCheckedChange = { selectedPermissions[permission.id] = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF1E88E5))
                        )
                    }
                    Divider(color = Color(0xFF2A2A3E))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onGenerateClicked(selectedPermissions.filter { it.value }.keys.toList()) },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Generate QR Code", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.QrCode, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}