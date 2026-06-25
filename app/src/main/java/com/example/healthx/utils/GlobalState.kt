package com.example.healthx.utils

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.healthx.data.local.SavedAccount

// This creates the global variable container
val LocalActiveAccount = staticCompositionLocalOf<SavedAccount?> {
    error("No active account provided!")
}