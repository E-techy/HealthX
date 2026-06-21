package com.example.healthx.ui.screens.chat

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthx.BuildConfig
import com.google.genai.Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// Represents a single message in the chat
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val attachedImage: Bitmap? = null
)

// Represents a Chat Session in the History Drawer
data class ChatSession(
    val id: String,
    val title: String,
    val timestamp: Long
)

class ChatViewModel : ViewModel() {

    // UI States
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatHistory: StateFlow<List<ChatSession>> = _chatHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isApiKeyMissing = MutableStateFlow(false)
    val isApiKeyMissing: StateFlow<Boolean> = _isApiKeyMissing.asStateFlow()

    private var genAiClient: Client? = null
    var currentChatId: String = ""
        private set

    private val systemInstruction = "System Instruction: You are HealthX, an advanced medical and fitness AI assistant. Base your answers strictly on the user's provided health data. Keep responses concise, clinical but friendly, and highly actionable."
    private var currentUserStats = "User Stats: Age 22, Weight 75kg, Daily Steps Avg: 6000. Medical History: None."

    init {
        loadDummyHistory()
        createNewChat()
    }

    private fun loadDummyHistory() {
        // Populating the sliding drawer with dummy past chats (Recent at top)
        _chatHistory.value = listOf(
            ChatSession("chat_002", "Diet plan for next week", System.currentTimeMillis() - 86400000),
            ChatSession("chat_003", "Medication side effects", System.currentTimeMillis() - 86400000 * 2),
            ChatSession("chat_004", "Morning Workout Routine", System.currentTimeMillis() - 86400000 * 5)
        )
    }

    fun createNewChat() {
        _messages.value = emptyList() // Clear screen
        initializeChatSession("chat_${UUID.randomUUID().toString().take(6)}")
    }

    fun loadChatSession(chatId: String) {
        // Placeholder: Fetch messages for this ID from Room DB
        _messages.value = listOf(
            ChatMessage(text = "Loaded history for $chatId (Placeholder)", isUser = false)
        )
        initializeChatSession(chatId)
    }

    private fun initializeChatSession(incomingChatId: String) {
        this.currentChatId = incomingChatId
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank()) {
            _isApiKeyMissing.value = true
            return
        }

        _isApiKeyMissing.value = false
        genAiClient = Client.builder().apiKey(apiKey).build()
        syncChatDataWithCloud()
    }

    fun sendMessage(userText: String, bitmap: Bitmap?) {
        val client = genAiClient ?: return

        val userMessage = ChatMessage(text = userText, isUser = true, attachedImage = bitmap)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // If this is the first message, add the session to the drawer
                if (_messages.value.size == 1) {
                    val newSession = ChatSession(currentChatId, userText.take(20) + "...", System.currentTimeMillis())
                    _chatHistory.value = listOf(newSession) + _chatHistory.value
                }

                val promptPayload = "$systemInstruction\n\nContext:\n$currentUserStats\n\nUser Question:\n$userText"

                // Using Gemini text generation (Image processing will require the image parameter in a future update)
                val response = client.models.generateContent("gemini-2.5-flash", promptPayload, null)

                val replyText = response.text() ?: "Sorry, I couldn't process that request."
                val aiMessage = ChatMessage(text = replyText, isUser = false)

                _messages.value = _messages.value + aiMessage
                saveLocally()
                pushToMongoDB()

            } catch (e: Exception) {
                Log.e("ChatViewModel", "AI Generation Failed", e)
                val errorMessage = ChatMessage(text = "Error connecting to AI: ${e.localizedMessage}", isUser = false)
                _messages.value = _messages.value + errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun syncChatDataWithCloud() {
        Log.i("HealthX_Sync", "[EXECUTION] Fetching chat $currentChatId from MongoDB Atlas...")
    }

    private fun saveLocally() {
        Log.i("HealthX_Sync", "[EXECUTION] Saving new message to Room Database (Local)...")
    }

    private fun pushToMongoDB() {
        Log.i("HealthX_Sync", "[EXECUTION] Pushing new message to MongoDB Atlas via backend API...")
    }
}