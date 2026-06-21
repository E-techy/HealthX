package com.example.healthx

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.example.healthx.ui.screens.chat.ChatScreen
import com.example.healthx.ui.theme.HealthXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // THIS LINE IS CRITICAL FOR THE KEYBOARD FIX
        // It tells Android to let Compose handle the screen boundaries
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            HealthXTheme {
                ChatScreen(
                    onNavigateToSettings = {
                        Log.d("Navigation", "User requested routing to API Key Settings")
                    }
                )
            }
        }
    }
}