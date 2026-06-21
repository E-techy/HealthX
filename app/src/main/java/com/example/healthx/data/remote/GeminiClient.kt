package com.example.healthx.data.remote

import com.google.genai.Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Client responsible for communicating with the Google Gemini AI using the latest google-genai SDK.
 */
class GeminiClient(private val apiKey: String) {

    // Instantiate the unified Client.
    // Explicitly set the API key.
    private val client = Client.builder()
        .apiKey(apiKey)
        .build()

    /**
     * Sends a test prompt to Gemini and returns the generated text response.
     */
    suspend fun testPrompt(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // Generate content using the new client architecture.
                // We are using gemini-2.5-flash, the latest lightweight model.
                val response = client.models.generateContent(
                    "gemini-2.5-flash",
                    prompt,
                    null
                )

                // Use the text() accessor method to retrieve the result.
                response.text() ?: "Error: Received an empty response from the AI."

            } catch (e: Exception) {
                "Failed to get response: ${e.localizedMessage}"
            }
        }
    }
}