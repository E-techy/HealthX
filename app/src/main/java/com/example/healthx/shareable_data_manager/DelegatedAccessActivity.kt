package com.example.healthx.shareable_data_manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.example.healthx.data.local.SavedAccount
import com.example.healthx.data.local.SessionManager
import com.example.healthx.ui.theme.HealthXTheme
import com.example.healthx.data.local.DelegatedSession
class DelegatedAccessActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            HealthXTheme {
                val context = LocalContext.current
                val sessionManager = remember { SessionManager(context) }
                val activeAccount by sessionManager.activeAccountFlow.collectAsState(initial = null)

                activeAccount?.let { account ->
                    DelegatedAccessMainScreen(
                        account = account,
                        onBack = { finish() },
                        onEnterGuestMode = { delegatedSession ->
                            sessionManager.enterDelegatedMode(delegatedSession)
                            finish() // Returns to Home Screen seamlessly
                        }
                    )
                }
            }
        }
    }
}