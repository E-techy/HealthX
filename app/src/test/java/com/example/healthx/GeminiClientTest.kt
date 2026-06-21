package com.example.healthx

import com.example.healthx.data.remote.GeminiClient
import kotlinx.coroutines.runBlocking
import org.junit.Test

class GeminiClientTest {

    @Test
    fun testGeminiConnection() = runBlocking {
        // 1. Replace this placeholder string with your actual Gemini API key from Google AI Studio
        val myApiKey = "AIzaSyA9cGqsmtBKyOp3rhCzY0ZxnaTPNebmZpQ"

        // 2. Initialize your client
        val client = GeminiClient(myApiKey)

        // 3. Define a simple test prompt
        val testPrompt = "Hello! Give me a one-sentence greeting for a health app named HealthX."

        println("Sending prompt to Gemini...")

        // 4. Call the function directly
        val response = client.testPrompt(testPrompt)

        // 5. Print the output straight to the Android Studio console
        println("\n--- Gemini Response ---")
        println(response)
        println("-----------------------\n")

        // Quick assertion to check if it succeeded or failed
        assert(!response.startsWith("Failed to get response")) { "Test failed with an error exception!" }
    }
}