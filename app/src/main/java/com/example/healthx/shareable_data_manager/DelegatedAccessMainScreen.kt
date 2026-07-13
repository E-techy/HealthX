package com.example.healthx.shareable_data_manager

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthx.data.local.SavedAccount
import com.example.healthx.shareable_data_manager.data.AccessPermissions
import com.example.healthx.utils.QRGenerator
import com.google.gson.Gson
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelegatedAccessMainScreen(
    account: SavedAccount,
    onBack: () -> Unit
) {
    val viewModel: DelegatedAccessViewModel = viewModel()
    val generateState by viewModel.generateHashState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for Default vs Generated Hash QR
    var activeQrPayload by remember { mutableStateOf(
        Gson().toJson(mapOf("category" to "ADD_FRIEND", "userId" to account.accountId))
    )}

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPermissionsSheet by remember { mutableStateOf(false) }
    var isShowingHash by remember { mutableStateOf(false) }

    // Generate QR Image when payload changes
    LaunchedEffect(activeQrPayload) {
        qrBitmap = QRGenerator.generateQRCode(data = activeQrPayload, size = 600)
    }

    // Handle API Error State
    LaunchedEffect(generateState) {
        if (generateState is UiState.Error) {
            snackbarHostState.showSnackbar(
                message = (generateState as UiState.Error).message,
                duration = SnackbarDuration.Short
            )
            viewModel.resetGenerateState()
        } else if (generateState is UiState.Success) {
            val hashData = (generateState as UiState.Success).data
            activeQrPayload = Gson().toJson(mapOf("category" to "SHARE_ACCESS", "hash" to hashData.hashId))
            isShowingHash = true
            showPermissionsSheet = false
            viewModel.resetGenerateState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Access Gateway", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                },
                actions = {
                    // Drawer trigger for next iteration
                    IconButton(onClick = { /* TODO: Open Drawer */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1A24))
            )
        },
        containerColor = Color(0xFF0A0A12)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isShowingHash) "Delegated Access Hash Active" else "Public Profile Connect",
                style = MaterialTheme.typography.titleLarge,
                color = if (isShowingHash) Color(0xFF00E676) else Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))

            // QR Code Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "Access QR Code",
                        modifier = Modifier
                            .size(280.dp)
                            .padding(16.dp)
                    )
                } else {
                    Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF1E88E5))
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (isShowingHash) {
                Button(
                    onClick = {
                        activeQrPayload = Gson().toJson(mapOf("category" to "ADD_FRIEND", "userId" to account.accountId))
                        isShowingHash = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
                ) {
                    Text("Revoke Display & Reset", fontSize = 16.sp)
                }
            } else {
                Button(
                    onClick = { showPermissionsSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                    modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
                ) {
                    Text("Share Granular Access", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showPermissionsSheet) {
        PermissionsBottomSheet(
            onDismiss = { showPermissionsSheet = false },
            onGenerateClicked = { selectedPermissions ->
                viewModel.generateHash(account.token, selectedPermissions)
            },
            isLoading = generateState is UiState.Loading
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsBottomSheet(
    onDismiss: () -> Unit,
    onGenerateClicked: (List<String>) -> Unit,
    isLoading: Boolean
) {
    // Keep track of toggled permissions locally in the sheet
    val selectedPermissions = remember { mutableStateMapOf<String, Boolean>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Configure Data Scopes",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(AccessPermissions.availablePermissions) { permission ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(text = permission.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = permission.description, color = Color.LightGray, fontSize = 12.sp)
                        }
                        Switch(
                            checked = selectedPermissions[permission.id] ?: false,
                            onCheckedChange = { isChecked ->
                                selectedPermissions[permission.id] = isChecked
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF1E88E5)
                            )
                        )
                    }
                    Divider(color = Color(0xFF2A2A3E))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val actions = selectedPermissions.filter { it.value }.keys.toList()
                    onGenerateClicked(actions)
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Generate Secure QR", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}