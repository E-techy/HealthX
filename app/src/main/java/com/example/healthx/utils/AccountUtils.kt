package com.example.healthx.utils

import android.content.Context
import com.example.healthx.data.local.SessionManager

object AccountUtils {

    /**
     * Removes all traces of a specific account from the device.
     * Can be called from the Account Picker or the Main Home Screen (Logout).
     */
    suspend fun completelyRemoveAccount(context: Context, accountId: String) {
        val sessionManager = SessionManager(context)

        // 1. Remove the JWT and account info from permanent DataStore
        sessionManager.removeAccount(accountId)

        // 2. Future expansion: You will add Room Database deletion logic here
        // e.g., chatDatabase.clearHistoryForUser(accountId)
    }
}