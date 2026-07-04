package com.example.healthx.qr_codes

import org.json.JSONObject

/**
 * A central utility object for building JSON payloads for various HealthX QR codes.
 */
object ProfileQRBuilder {

    /**
     * Generates the JSON string payload for sharing a user's basic profile.
     * * @param accountId The unique user ID of the account.
     * @param name The display name of the user.
     * @param email The email address of the user.
     * @return A stringified JSON object ready to be encoded into a QR Bitmap.
     */
    fun buildShareProfilePayload(accountId: String, name: String, email: String): String {
        return JSONObject().apply {
            // This category key tells the QR Scanner how to process this specific QR code
            put("HealthX_category", "profile_sharing")
            put("accountId", accountId)
            put("name", name)
            put("email", email)
        }.toString()
    }

    // Example for your future use cases:
    // fun buildChatPayload(...) { ... put("HealthX_category", "chat_connect") }
    // fun buildHealthRecordPayload(...) { ... put("HealthX_category", "health_record") }
}