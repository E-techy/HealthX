package com.example.healthx.ui.screens.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.example.healthx.ui.theme.HealthXTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allows drawing behind system bars for that cinematic feel
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            HealthXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0A12) // Forced Dark Background
                ) {
                    SettingsScreen(onBack = { finish() })
                }
            }
        }
    }
}