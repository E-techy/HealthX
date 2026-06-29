package com.example.healthx.ui.subscription

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.healthx.MainActivity

class SubscriptionActivity : ComponentActivity() {

    private val viewModel: SubscriptionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if the Intent passed a specific subscription ID (from the "Subscribe" button)
        val passedSubscriptionId = intent.getStringExtra("EXTRA_SUBSCRIPTION_ID")

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                SubscriptionScreenContainer(
                    viewModel = viewModel,
                    initialSubscriptionId = passedSubscriptionId
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreenContainer(
    viewModel: SubscriptionViewModel,
    initialSubscriptionId: String?
) {
    val context = LocalContext.current

    // State to track if we are looking at a specific subscription or the main list
    var activeSubscriptionId by remember { mutableStateOf(initialSubscriptionId) }

    // Initial Data Fetching based on entry point
    LaunchedEffect(activeSubscriptionId) {
        if (activeSubscriptionId != null) {
            viewModel.fetchSubscriptionDetails(activeSubscriptionId!!)
        } else {
            viewModel.fetchAllSubscriptions()
        }
    }

    // Custom Back Navigation Logic
    BackHandler {
        if (activeSubscriptionId != null) {
            // 1. If in Detail View, go back to List View
            activeSubscriptionId = null
        } else {
            // 2. If in List View, route to MainActivity and kill this activity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            (context as Activity).finish()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (activeSubscriptionId == null) "Subscriptions" else "Subscription Details")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (activeSubscriptionId != null) {
                                activeSubscriptionId = null // Go to list
                            } else {
                                // Go to main activity
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                                (context as Activity).finish()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFF121212)),
            contentAlignment = Alignment.Center
        ) {
            if (activeSubscriptionId == null) {
                // Placeholder for the Main List UI
                Text("Main Subscription List View", color = Color.Gray)
            } else {
                // Placeholder for the Specific Detail UI
                Text("Viewing details for ID: $activeSubscriptionId", color = Color.Gray)
            }
        }
    }
}