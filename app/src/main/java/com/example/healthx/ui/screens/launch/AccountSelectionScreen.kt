package com.example.healthx.ui.screens.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.launch

@Composable
fun AccountSelectionScreen(
    accounts: List<SavedAccount>,
    onAccountSelected: (SavedAccount) -> Unit,
    onAddNewAccount: () -> Unit,
    onRemoveAccount: (String, () -> Unit) -> Unit // Updated to accept a success callback
) {
    // States for the Dialog and Success Message
    var accountToDelete by remember { mutableStateOf<SavedAccount?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Confirmation Dialog Popup
    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Remove Account", color = Color.White) },
            text = {
                Text(
                    text = "Are you sure you want to remove ${accountToDelete!!.name} (${accountToDelete!!.email}) from this device?",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = accountToDelete!!.accountId
                        accountToDelete = null // Close dialog

                        // Execute deletion and show success message
                        onRemoveAccount(id) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Account deleted successfully.")
                            }
                        }
                    }
                ) {
                    Text("Yes, Remove", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                text = "Choose an Account",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(accounts) { account ->
                    AccountCard(
                        account = account,
                        onClick = { onAccountSelected(account) },
                        onRemoveClick = { accountToDelete = account } // Trigger the popup instead of instant deletion
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onAddNewAccount,
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Account")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add new account")
                    }
                }
            }
        }
    }
}

@Composable
fun AccountCard(
    account: SavedAccount,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E1E))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Image or Initial Placeholder
        if (!account.profilePhotoUrl.isNullOrBlank()) {
            coil.compose.SubcomposeAsyncImage(
                model = account.profilePhotoUrl,
                contentDescription = "Profile Picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(50.dp).clip(CircleShape),
                loading = {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = account.initials, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF2C2C2C)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = account.initials, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = account.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(text = account.email, color = Color.Gray, fontSize = 14.sp)
        }

        IconButton(onClick = onRemoveClick) {
            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Remove Account", tint = Color.Gray)
        }
    }
}