package com.example.healthx.docs_manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.example.healthx.docs_manager.ui.DocsDashboardScreen
import com.example.healthx.ui.theme.HealthXTheme

class DocsManagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            HealthXTheme {
                DocsDashboardScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}