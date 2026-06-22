package com.example.healthx.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "healthx_auth_prefs")

data class SavedAccount(
    val accountId: String,
    val email: String,
    val name: String,
    val token: String,
    val profilePhotoUrl: String? = null, // Added Profile Photo URL
    val isGuest: Boolean = false
) {
    // Helper to get the first letter for the default avatar
    val initials: String
        get() = if (name.isNotBlank()) name.take(1).uppercase() else "?"
}

class SessionManager(private val context: Context) {
    private val gson = Gson()
    private val ACCOUNTS_KEY = stringPreferencesKey("saved_accounts_list")
    private val ACTIVE_ACCOUNT_ID_KEY = stringPreferencesKey("active_account_id")

    // Get the list of all logged-in accounts
    val savedAccountsFlow: Flow<List<SavedAccount>> = context.dataStore.data.map { prefs ->
        val json = prefs[ACCOUNTS_KEY] ?: "[]"
        val type = object : TypeToken<List<SavedAccount>>() {}.type
        gson.fromJson(json, type)
    }

    // Get the currently active account
    val activeAccountFlow: Flow<SavedAccount?> = context.dataStore.data.map { prefs ->
        val activeId = prefs[ACTIVE_ACCOUNT_ID_KEY]
        val json = prefs[ACCOUNTS_KEY] ?: "[]"
        val type = object : TypeToken<List<SavedAccount>>() {}.type
        val accounts: List<SavedAccount> = gson.fromJson(json, type)
        accounts.find { it.accountId == activeId }
    }

    suspend fun saveAccountAndSetActive(account: SavedAccount) {
        context.dataStore.edit { prefs ->
            val json = prefs[ACCOUNTS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<SavedAccount>>() {}.type
            val accounts: MutableList<SavedAccount> = gson.fromJson(json, type)

            // Remove if exists to update token, then add
            accounts.removeAll { it.accountId == account.accountId }
            accounts.add(account)

            prefs[ACCOUNTS_KEY] = gson.toJson(accounts)
            prefs[ACTIVE_ACCOUNT_ID_KEY] = account.accountId
        }
    }

    suspend fun switchActiveAccount(accountId: String) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_ACCOUNT_ID_KEY] = accountId
        }
    }
}