package com.example.healthx.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage // Coil explicitly imported

import com.example.healthx.MainActivity
import com.example.healthx.data.local.AppDatabase
import com.example.healthx.notification_manager.NotificationEntity
import com.example.healthx.notification_manager.NotificationViewModel
import com.example.healthx.notification_manager.NotificationViewModelFactory

class CommonNotificationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fully wired Room DB connection
        val dao = AppDatabase.getDatabase(this).notificationDao()
        val viewModel: NotificationViewModel by viewModels { NotificationViewModelFactory(dao) }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                NotificationScreenContainer(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreenContainer(viewModel: NotificationViewModel) {
    val context = LocalContext.current
    val notifications by viewModel.notifications.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var selectedNotification by remember { mutableStateOf<NotificationEntity?>(null) }

    BackHandler {
        if (selectedNotification != null) {
            selectedNotification = null
        } else {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            (context as Activity).finish()
        }
    }

    Scaffold(
        topBar = {
            if (selectedNotification == null) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search notifications...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 16.dp, top = 8.dp, bottom = 8.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color(0xFF1E1E1E),
                                unfocusedContainerColor = Color(0xFF1E1E1E)
                            )
                        )
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(selectedNotification!!.category) },
                    navigationIcon = {
                        IconButton(onClick = { selectedNotification = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFF121212))
        ) {
            if (selectedNotification == null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = notifications,
                        key = { it.notificationId } // 'it' is fine here for the key selector
                    ) { notification -> // You named the item 'notification' here
                        NotificationCard(
                            notification = notification,
                            onClick = {
                                // Use 'notification' instead of 'it'
                                viewModel.markAsRead(notification.notificationId)
                                selectedNotification = notification
                            },
                            onDelete = {
                                // Use 'notification' instead of 'it'
                                viewModel.deleteNotification(notification.notificationId)
                            }
                        )
                    }
                }
            } else {
                NotificationDetailScreen(notification = selectedNotification!!)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCard(
    notification: NotificationEntity,
    onClick: (NotificationEntity) -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFB3261E), RoundedCornerShape(12.dp))
                    .padding(start = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(notification) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (notification.isRead) Color(0xFF1E1E1E) else Color(0xFF2C2C2C)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notification.title,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notification.smallDescription,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun NotificationDetailScreen(notification: NotificationEntity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = notification.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = notification.fullDescription ?: notification.smallDescription,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.LightGray
        )

        // Coil AsyncImage fully integrated to handle remote URLs
        if (!notification.imageUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(24.dp))
            AsyncImage(
                model = notification.imageUrl,
                contentDescription = "Notification Image",
                contentScale = ContentScale.Crop, // Crops image to fit the bounds neatly
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}