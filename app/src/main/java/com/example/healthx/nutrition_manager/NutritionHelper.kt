package com.example.healthx.nutrition_manager

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

object NutritionHelper {

    // Fake user profile to send to the AI
    fun getFakeUserProfile(): String {
        return """
            {
                "name": "Ashutosh Kumar Singh",
                "age": 20,
                "gender": "MALE",
                "allergies": [],
                "currentGoal": "MUSCLE_GAIN",
                "targetCalories": 2800
            }
        """.trimIndent()
    }

    // Helper to convert plain text to Multipart RequestBody
    fun createPartFromString(text: String): RequestBody {
        return text.toRequestBody("text/plain".toMediaTypeOrNull())
    }

    /* * TODO: Uncomment these when testing non-PRO accounts
     * * fun getApiKeyPart(): RequestBody = createPartFromString("AIzaSyYourSecretKeyHere...")
     * fun getModelNamePart(): RequestBody = createPartFromString("gemini-2.5-flash")
     */
}