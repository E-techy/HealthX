package com.example.healthx.shareable_data_manager

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.healthx.BuildConfig
import com.example.healthx.data.local.DelegatedSession
import com.example.healthx.data.local.SavedAccount
import com.example.healthx.shareable_data_manager.data.*
import com.example.healthx.utils.QRGenerator
import com.google.gson.Gson
import kotlinx.coroutines.launch

enum class CurrentScreen {
    QR_GENERATOR,
    ACTIVE_LINKS,
    GRANTED_ACCESS, // People who can see my data
    RECEIVED_ACCESS, // Profiles I can View
    BLOCKLIST
}

// HELPER: Formats relative backend paths into full URLs for Coil
fun getFullImageUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (path.startsWith("http")) return path
    val baseUrl = BuildConfig.BASE_URL.removeSuffix("/")
    val safePath = if (path.startsWith("/")) path else "/$path"
    return baseUrl + safePath
}

// HELPER: Translates "SEE_NUTRITION" into "View Nutrition Logs"
fun getHumanReadablePermission(actionId: String): String {
    return AccessPermissions.availablePermissions.find { it.id == actionId }?.title ?: actionId
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelegatedAccessMainScreen(
    account: SavedAccount,
    onBack: () -> Unit,
    onEnterGuestMode: (DelegatedSession) -> Unit
) {
    val viewModel: DelegatedAccessViewModel = viewModel()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Set Default Screen to Share Access
    var currentScreen by remember { mutableStateOf(CurrentScreen.QR_GENERATOR) }
    val actionState by viewModel.actionState.collectAsState()

    LaunchedEffect(actionState) {
        if (actionState is UiState.Success) {
            snackbarHostState.showSnackbar((actionState as UiState.Success).data)
            viewModel.resetActionState()
        } else if (actionState is UiState.Error) {
            snackbarHostState.showSnackbar((actionState as UiState.Error).message)
            viewModel.resetActionState()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFF141414), modifier = Modifier.width(300.dp)) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Access Gateway", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, bottom = 16.dp))
                Divider(color = Color(0xFF2C2C2C))

                DrawerMenuButton(icon = Icons.Outlined.QrCode, label = "Share Access", isSelected = currentScreen == CurrentScreen.QR_GENERATOR) {
                    currentScreen = CurrentScreen.QR_GENERATOR; scope.launch { drawerState.close() }
                }
                DrawerMenuButton(icon = Icons.Outlined.People, label = "Profiles I Can View", isSelected = currentScreen == CurrentScreen.RECEIVED_ACCESS) {
                    currentScreen = CurrentScreen.RECEIVED_ACCESS; viewModel.fetchReceivedAccess(account.token); scope.launch { drawerState.close() }
                }
                Divider(color = Color(0xFF2C2C2C), modifier = Modifier.padding(vertical = 8.dp))
                DrawerMenuButton(icon = Icons.Outlined.Security, label = "Who Has Access", isSelected = currentScreen == CurrentScreen.GRANTED_ACCESS) {
                    currentScreen = CurrentScreen.GRANTED_ACCESS; viewModel.fetchGrantedAccess(account.token); scope.launch { drawerState.close() }
                }
                DrawerMenuButton(icon = Icons.Outlined.Link, label = "Manage Active Links", isSelected = currentScreen == CurrentScreen.ACTIVE_LINKS) {
                    currentScreen = CurrentScreen.ACTIVE_LINKS; viewModel.fetchMyHashes(account.token); scope.launch { drawerState.close() }
                }
                DrawerMenuButton(icon = Icons.Outlined.Block, label = "Blocked Users", isSelected = currentScreen == CurrentScreen.BLOCKLIST) {
                    currentScreen = CurrentScreen.BLOCKLIST; viewModel.fetchBlocklist(account.token); scope.launch { drawerState.close() }
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
                                CurrentScreen.GRANTED_ACCESS -> "Who Has Access"
                                CurrentScreen.RECEIVED_ACCESS -> "Profiles I Can View"
                                CurrentScreen.QR_GENERATOR -> "Share Access"
                                CurrentScreen.ACTIVE_LINKS -> "Active Links"
                                CurrentScreen.BLOCKLIST -> "Blocked Users"
                            }, color = Color.White
                        )
                    },
                    // SWAPPED NAV LAYOUT: Hamburger on Left, Close/Back on Right
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, tint = Color.White, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, tint = Color.White, contentDescription = "Close Dashboard")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1A24))
                )
            },
            containerColor = Color(0xFF0A0A12)
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    CurrentScreen.GRANTED_ACCESS -> GrantedAccessScreen(account.token, viewModel)
                    CurrentScreen.RECEIVED_ACCESS -> ReceivedAccessScreen(account.token, viewModel, onEnterGuestMode)
                    CurrentScreen.QR_GENERATOR -> QRCodeGeneratorScreen(account, viewModel, snackbarHostState)
                    CurrentScreen.ACTIVE_LINKS -> ActiveLinksScreen(account.token, viewModel)
                    CurrentScreen.BLOCKLIST -> BlocklistScreen(account.token, viewModel)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// RECEIVED ACCESS (USER B: Profiles I can View)
// -------------------------------------------------------------
@Composable
fun ReceivedAccessScreen(token: String, viewModel: DelegatedAccessViewModel, onEnterGuestMode: (DelegatedSession) -> Unit) {
    val receivedState by viewModel.receivedAccessState.collectAsState()
    var selectedUserForReport by remember { mutableStateOf<ReceivedAccessProfile?>(null) }

    LaunchedEffect(Unit) { if (receivedState is UiState.Idle) viewModel.fetchReceivedAccess(token) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = receivedState) {
            is UiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is UiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message, color = Color.Red) }
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("You don't have access to anyone's data.", color = Color.Gray) }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(state.data) { item ->
                            ReceivedAccessCard(
                                item = item,
                                onAccessData = {
                                    val activeStrings = item.activePermissions.filter { it.isActive }.map { it.action }
                                    onEnterGuestMode(DelegatedSession(item.user.userId, item.user.name, item.user.profileImageUri, activeStrings))
                                },
                                onReport = { selectedUserForReport = item.user }
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }

    if (selectedUserForReport != null) {
        BlockUserDialog(
            user = selectedUserForReport!!,
            onDismiss = { selectedUserForReport = null },
            onBlock = { reason, notes ->
                viewModel.blockUser(token, selectedUserForReport!!.userId, reason, notes)
                selectedUserForReport = null
            }
        )
    }
}

@Composable
fun ReceivedAccessCard(item: ReceivedAccessItem, onAccessData: () -> Unit, onReport: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF2C2C2C)), contentAlignment = Alignment.Center) {
                        val fullUrl = getFullImageUrl(item.user.profileImageUri)
                        if (fullUrl != null) {
                            AsyncImage(model = fullUrl, contentDescription = "Profile", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Text(item.user.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(item.user.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Connected: ${item.connectedAt.take(10)}", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                // 3-Dot Menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1E1E1E))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Report & Disconnect", color = Color.Red, fontWeight = FontWeight.SemiBold) },
                            onClick = { showMenu = false; onReport() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Permissions Granted to You:", color = Color.LightGray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Action Chips Mapping
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.activePermissions.filter { it.isActive }.forEach { perm ->
                    Surface(color = Color(0xFF1E88E5).copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = getHumanReadablePermission(perm.action),
                            color = Color(0xFF64B5F6),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAccessData, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))) {
                Text("Access Data", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// BLOCK USER DIALOG (With Category Dropdown)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockUserDialog(user: ReceivedAccessProfile, onDismiss: () -> Unit, onBlock: (String, String) -> Unit) {
    var reason by remember { mutableStateOf("PRIVACY_CONCERN") }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val reasonOptions = mapOf(
        "PRIVACY_CONCERN" to "Privacy Concern",
        "SPAM" to "Spam / Unwanted",
        "INAPPROPRIATE_BEHAVIOR" to "Inappropriate Behavior",
        "NO_LONGER_FRIENDS" to "No Longer Friends",
        "OTHER" to "Other Reason"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Report ${user.name}?", color = Color.White) },
        text = {
            Column {
                Text("This will sever the connection and block them. They will not be notified.", color = Color.LightGray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))

                // Reason Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = reasonOptions[reason] ?: "Select Reason",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF2C2C2C))
                    ) {
                        reasonOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = Color.White) },
                                onClick = { reason = key; expanded = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Optional Notes") }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onBlock(reason, notes) }) { Text("Block & Revoke", color = Color.Red, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}

// -------------------------------------------------------------
// GRANTED ACCESS (USER A: People who can see my data)
// -------------------------------------------------------------
@Composable
fun GrantedAccessScreen(token: String, viewModel: DelegatedAccessViewModel) {
    val grantedState by viewModel.grantedAccessState.collectAsState()
    var selectedUserForEdit by remember { mutableStateOf<GrantedAccessItem?>(null) }
    var selectedUserForBlock by remember { mutableStateOf<ReceivedAccessProfile?>(null) }

    LaunchedEffect(Unit) { if (grantedState is UiState.Idle) viewModel.fetchGrantedAccess(token) }

    when (val state = grantedState) {
        is UiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is UiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message, color = Color.Red) }
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No one has access to your data.", color = Color.Gray) }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(state.data) { item ->
                        GrantedAccessCard(
                            item = item,
                            onEditClick = { selectedUserForEdit = item },
                            onBlockClick = { selectedUserForBlock = item.user }
                        )
                    }
                }
            }
        }
        else -> {}
    }

    if (selectedUserForEdit != null) {
        EditPermissionsSheet(
            item = selectedUserForEdit!!,
            onDismiss = { selectedUserForEdit = null },
            onSave = { updatedPermissions ->
                viewModel.updatePermissions(token, selectedUserForEdit!!.user.userId, updatedPermissions)
                selectedUserForEdit = null
            }
        )
    }

    if (selectedUserForBlock != null) {
        BlockUserDialog(
            user = selectedUserForBlock!!,
            onDismiss = { selectedUserForBlock = null },
            onBlock = { reason, notes ->
                viewModel.blockUser(token, selectedUserForBlock!!.userId, reason, notes)
                selectedUserForBlock = null
            }
        )
    }
}

@Composable
fun GrantedAccessCard(item: GrantedAccessItem, onEditClick: () -> Unit, onBlockClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2C2C2C)), contentAlignment = Alignment.Center) {
                        val fullUrl = getFullImageUrl(item.user.profileImageUri)
                        if (fullUrl != null) {
                            AsyncImage(model = fullUrl, contentDescription = "Profile", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Text(item.user.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(item.user.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Connected: ${item.connectedAt.take(10)}", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onBlockClick) { Icon(Icons.Outlined.Block, tint = Color(0xFFE53935), contentDescription = "Block") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onEditClick, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))) {
                Text("Manage Permissions", color = Color.White)
            }
        }
    }
}

// -------------------------------------------------------------
// BLOCKLIST SCREEN
// -------------------------------------------------------------
@Composable
fun BlocklistScreen(token: String, viewModel: DelegatedAccessViewModel) {
    val blocklistState by viewModel.blocklistState.collectAsState()

    when (val state = blocklistState) {
        is UiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is UiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message, color = Color.Red) }
        is UiState.Success -> {
            val users = state.data
            if (users.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No blocked users.", color = Color.Gray) }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(users) { user ->
                        BlocklistedUserRow(user = user, onUnblock = { viewModel.unblockUser(token, user.userId) })
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
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).background(Color(0xFF1A1A2E), RoundedCornerShape(12.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2C2C2C)), contentAlignment = Alignment.Center) {
                val fullUrl = getFullImageUrl(user.profileImageUri)
                if (fullUrl != null) {
                    AsyncImage(model = fullUrl, contentDescription = "Profile", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(user.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(user.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        TextButton(onClick = onUnblock) { Text("Unblock", color = Color(0xFF1E88E5), fontWeight = FontWeight.Bold) }
    }
}

// -------------------------------------------------------------
// HELPER COMPONENTS (Unchanged behavior, kept for completeness)
// -------------------------------------------------------------
@Composable
fun DrawerMenuButton(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = label, tint = if (isSelected) Color(0xFF1E88E5) else Color.White) },
        label = { Text(label, color = if (isSelected) Color(0xFF1E88E5) else Color.White, fontSize = 16.sp) },
        selected = isSelected, onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, selectedContainerColor = Color(0xFF1E88E5).copy(alpha = 0.1f)),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
fun QRCodeGeneratorScreen(account: SavedAccount, viewModel: DelegatedAccessViewModel, snackbarHostState: SnackbarHostState) {
    // Exact same implementation from previous code...
    val generateState by viewModel.generateHashState.collectAsState()
    var activeQrPayload by remember { mutableStateOf(Gson().toJson(mapOf("category" to "ADD_FRIEND", "userId" to account.accountId))) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPermissionsSheet by remember { mutableStateOf(false) }
    var isCustomQrActive by remember { mutableStateOf(false) }

    LaunchedEffect(activeQrPayload) { qrBitmap = QRGenerator.generateQRCode(data = activeQrPayload, size = 600) }
    LaunchedEffect(generateState) {
        if (generateState is UiState.Error) { snackbarHostState.showSnackbar((generateState as UiState.Error).message); viewModel.resetGenerateState() }
        else if (generateState is UiState.Success) {
            val hashData = (generateState as UiState.Success).data
            activeQrPayload = Gson().toJson(mapOf("category" to "SHARE_ACCESS", "hash" to hashData.hashId))
            isCustomQrActive = true; showPermissionsSheet = false; viewModel.resetGenerateState()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = if (isCustomQrActive) "Custom Share Link Ready" else "Your Default QR Code", style = MaterialTheme.typography.titleMedium, color = if (isCustomQrActive) Color(0xFF00E676) else Color.LightGray)
        Spacer(modifier = Modifier.height(24.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(8.dp)) {
            if (qrBitmap != null) Image(bitmap = qrBitmap!!.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(260.dp).padding(16.dp))
            else Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        Spacer(modifier = Modifier.height(48.dp))
        if (isCustomQrActive) {
            Button(onClick = { activeQrPayload = Gson().toJson(mapOf("category" to "ADD_FRIEND", "userId" to account.accountId)); isCustomQrActive = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(0.9f).height(56.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp)); Text("Reset to Default QR", fontSize = 16.sp, color = Color.White)
            }
        } else {
            Button(onClick = { showPermissionsSheet = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(0.9f).height(56.dp)) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp)); Text("Create Custom QR Code", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    if (showPermissionsSheet) PermissionsBottomSheet(onDismiss = { showPermissionsSheet = false }, onGenerateClicked = { selectedPermissions -> viewModel.generateHash(account.token, selectedPermissions) }, isLoading = generateState is UiState.Loading)
}

// ==========================================
// 2. ACTIVE LINKS SCREEN (HASHES)
// ==========================================
@Composable
fun ActiveLinksScreen(token: String, viewModel: DelegatedAccessViewModel) {
    val hashesState by viewModel.myHashesState.collectAsState()

    // Dialog States
    var selectedHashForQR by remember { mutableStateOf<ShareableHash?>(null) }
    var selectedHashForEdit by remember { mutableStateOf<ShareableHash?>(null) }

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
                            onShowQr = { selectedHashForQR = hash },
                            onEdit = { selectedHashForEdit = hash },
                            onDelete = { viewModel.deleteHash(token, hash.hashId) }
                        )
                    }
                }
            }
        }
        else -> {}
    }

    // Popups
    if (selectedHashForQR != null) {
        HashQrDialog(
            hashId = selectedHashForQR!!.hashId,
            onDismiss = { selectedHashForQR = null }
        )
    }

    if (selectedHashForEdit != null) {
        EditHashPermissionsSheet(
            hash = selectedHashForEdit!!,
            onDismiss = { selectedHashForEdit = null },
            onSave = { updatedActions ->
                viewModel.updateHashActions(token, selectedHashForEdit!!.hashId, updatedActions)
                selectedHashForEdit = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HashCard(hash: ShareableHash, onToggleStatus: () -> Unit, onShowQr: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isActive = hash.status == "ACTIVE"
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header: ID and Action Buttons
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Link ID: ${hash.hashId.take(8).uppercase()}",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onShowQr, modifier = Modifier.size(36.dp).background(Color(0xFF2C2C2C), CircleShape)) {
                        Icon(Icons.Default.QrCode, contentDescription = "Show QR", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp).background(Color(0xFF2C2C2C), CircleShape)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Permissions", tint = Color(0xFF1E88E5), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp).background(Color(0xFF2C2C2C), CircleShape)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Permissions attached to this link:", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Action Chips Mapping
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                hash.actions.forEach { action ->
                    Surface(color = Color(0xFF1E88E5).copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = getHumanReadablePermission(action),
                            color = Color(0xFF64B5F6),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFF2C2C2C))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Status Toggle
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(if (isActive) "Link is Active (Scannable)" else "Link is Paused", color = if (isActive) Color(0xFF00E676) else Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Switch(
                    checked = isActive,
                    onCheckedChange = { onToggleStatus() },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF1E88E5))
                )
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOGS: Hash QR Viewer & Hash Permissions Editor
// -------------------------------------------------------------
@Composable
fun HashQrDialog(hashId: String, onDismiss: () -> Unit) {
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(hashId) {
        val payload = Gson().toJson(mapOf("category" to "SHARE_ACCESS", "hash" to hashId))
        qrBitmap = QRGenerator.generateQRCode(data = payload, size = 600)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Link QR Code", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Show this to your friend to instantly connect.", color = Color.LightGray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    if (qrBitmap != null) {
                        Image(bitmap = qrBitmap!!.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(220.dp).padding(16.dp))
                    } else {
                        Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Color(0xFF1E88E5), fontWeight = FontWeight.Bold) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHashPermissionsSheet(hash: ShareableHash, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    val selectedPermissions = remember {
        mutableStateMapOf<String, Boolean>().apply {
            AccessPermissions.availablePermissions.forEach { put(it.id, hash.actions.contains(it.id)) }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF1A1A2E)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text("Edit Link Permissions", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
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
                onClick = { onSave(selectedPermissions.filter { it.value }.keys.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Save Updates", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPermissionsSheet(item: GrantedAccessItem, onDismiss: () -> Unit, onSave: (List<ActivePermission>) -> Unit) {
    // Exact same implementation...
    val editablePermissions = remember { mutableStateListOf<ActivePermission>().apply { addAll(item.permissions.map { it.copy() }) } }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF1A1A2E)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text("Edit Permissions for ${item.user.name}", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(editablePermissions.size) { index ->
                    val perm = editablePermissions[index]
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(getHumanReadablePermission(perm.action), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Switch(checked = perm.isActive, onCheckedChange = { editablePermissions[index] = perm.copy(isActive = it) }, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF1E88E5)))
                    }
                    Divider(color = Color(0xFF2A2A3E))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onSave(editablePermissions) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))) { Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsBottomSheet(onDismiss: () -> Unit, onGenerateClicked: (List<String>) -> Unit, isLoading: Boolean) {
    // Exact same implementation...
    val selectedPermissions = remember { mutableStateMapOf<String, Boolean>() }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF1A1A2E), dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(text = "Select Information to Share", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(AccessPermissions.availablePermissions) { permission ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(text = permission.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = permission.description, color = Color.LightGray, fontSize = 12.sp)
                        }
                        Switch(checked = selectedPermissions[permission.id] ?: false, onCheckedChange = { selectedPermissions[permission.id] = it }, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF1E88E5)))
                    }
                    Divider(color = Color(0xFF2A2A3E))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onGenerateClicked(selectedPermissions.filter { it.value }.keys.toList()) }, enabled = !isLoading, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else { Text("Generate QR Code", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.width(8.dp)); Icon(Icons.Default.QrCode, contentDescription = null) }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}