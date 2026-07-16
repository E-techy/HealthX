package com.example.healthx.data.local

data class DelegatedSession(
    val targetUserId: String,
    val name: String,
    val profilePhotoUrl: String?,
    val activePermissions: List<String>
) {
    // Helper to instantly check if an action is allowed
    fun hasPermission(action: String): Boolean {
        return activePermissions.contains("ALL") || activePermissions.contains(action)
    }
}