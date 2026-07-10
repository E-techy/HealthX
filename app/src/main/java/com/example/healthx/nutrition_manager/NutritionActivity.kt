package com.example.healthx.nutrition_manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.example.healthx.ui.theme.HealthXTheme

class NutritionActivity : ComponentActivity() {

    // Instantiate the ViewModel that survives configuration changes
    private val viewModel: NutritionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allows the cinematic dark theme to draw behind the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            HealthXTheme {
                // This launches the Compose flow we built previously,
                // containing the Home -> Scanner -> Amount -> Loading -> Success/Error screens.
                NutritionManagerApp(viewModel = viewModel)
            }
        }
    }
}