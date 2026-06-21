package com.example.healthx.data.model
import java.util.Date

data class UserAuth(
    val accountId: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val isEmailVerified: Boolean = false,
    val mfaEnabled: Boolean = false,
    val lastLoginTimestamp: Date? = null,
    val accountStatus: AccountStatus = AccountStatus.ACTIVE
)
enum class AccountStatus { ACTIVE, SUSPENDED, DEACTIVATED }