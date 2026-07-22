package com.example.healthx.docs_manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.healthx.BuildConfig
import com.example.healthx.docs_manager.data.AccessDetailsData
import com.example.healthx.docs_manager.ui.DocsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocAccessManagerDialog(
    docId: String,
    viewModel: DocsViewModel,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var accessData by remember { mutableStateOf<AccessDetailsData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Input States
    var passwordInput by remember { mutableStateOf("") }
    var targetUserIdInput by remember { mutableStateOf("") }

    // Fetch initial data
    val loadData = {
        coroutineScope.launch {
            isLoading = true
            accessData = viewModel.getAccessDetails(docId)
            isLoading = false
        }
    }

    LaunchedEffect(docId) { loadData() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(top = 40.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color(0xFF121212)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Manage Access", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                }

                Divider(color = Color(0xFF2C2C2C), modifier = Modifier.padding(vertical = 16.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (accessData != null) {
                    val data = accessData!!
                    LazyColumn(modifier = Modifier.fillMaxSize()) {

                        // 1. PUBLIC ACCESS SECTION
                        item {
                            Text("Public Link", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (data.isPublic && data.publicUrl != null) {
                                val fullUrl = "${BuildConfig.BASE_URL.removeSuffix("/")}${data.publicUrl}"
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp)).padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(fullUrl, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(fullUrl)) }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { coroutineScope.launch { viewModel.revokePublic(docId); loadData() } },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                                ) { Text("Revoke Public Link", color = Color.White) }
                            } else {
                                Button(
                                    onClick = { coroutineScope.launch { viewModel.makePublic(docId); loadData() } }
                                ) { Text("Generate Public Link") }
                            }
                            Divider(color = Color(0xFF2C2C2C), modifier = Modifier.padding(vertical = 24.dp))
                        }

                        // 2. PASSWORD SECTION
                        item {
                            Text("Password Protection", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (data.isPasswordProtected) {
                                Text("✅ This document is currently password protected.", color = Color(0xFF81C784))
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    placeholder = { Text(if (data.isPasswordProtected) "Update Password" else "Set Password") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.setPassword(docId, passwordInput)
                                            passwordInput = ""
                                            loadData()
                                        }
                                    },
                                    enabled = passwordInput.isNotBlank()
                                ) { Text("Set") }
                            }
                            Divider(color = Color(0xFF2C2C2C), modifier = Modifier.padding(vertical = 24.dp))
                        }

                        // 3. USER SHARING SECTION
                        item {
                            Text("Share with specific users", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = targetUserIdInput,
                                    onValueChange = { targetUserIdInput = it },
                                    placeholder = { Text("Enter User ID") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.shareUser(docId, targetUserIdInput)
                                            targetUserIdInput = ""
                                            loadData()
                                        }
                                    },
                                    enabled = targetUserIdInput.isNotBlank()
                                ) { Text("Share") }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // SHARED USERS LIST
                        items(data.sharedUsers) { user ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp)).padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(user.name, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(user.email, color = Color.Gray, fontSize = 12.sp)
                                }
                                IconButton(
                                    onClick = { coroutineScope.launch { viewModel.revokeShare(docId, user._id); loadData() } }
                                ) { Icon(Icons.Default.Delete, contentDescription = "Revoke", tint = Color(0xFFE53935)) }
                            }
                        }
                    }
                } else {
                    Text("Failed to load access details.", color = Color.Red)
                }
            }
        }
    }
}