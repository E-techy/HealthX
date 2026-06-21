package com.example.healthx

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.healthx.ui.screens.ScannerScreen
import com.example.healthx.ui.theme.HealthXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthXTheme {
                ScannerScreen(
                    onAddButtonClicked = { parsedData ->
                        // Verify backend collection pipelines via standard output
                        Log.d("ScannerSuccess", "Data intercepted for downstream processors: $parsedData")
                    }
                )
            }
        }
    }
}