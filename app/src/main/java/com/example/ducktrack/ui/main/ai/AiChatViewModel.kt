package com.example.ducktrack.ui.main.ai

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.data.DataAggregator
import com.example.ducktrack.data.GeminiHelper
import com.example.ducktrack.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

// Thêm timestamp mặc định là thời điểm hiện tại
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class AiChatViewModel(application: Application) : AndroidViewModel(application) {

    private val userPrefs = UserPreferences(application)
    private val dataAggregator = DataAggregator(application)
    private var tts: TextToSpeech? = null
    private val chatFile = File(application.filesDir, "chat_history.json")

    val duckName = userPrefs.duckName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Giáo Sư Vịt")

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf())
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        initTextToSpeech(application)
        loadChatHistory() // Tải tin nhắn cũ ngay khi mở
    }

    // --- XỬ LÝ LƯU TRỮ (PERSISTENCE) ---
    private fun loadChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            if (chatFile.exists()) {
                try {
                    val content = chatFile.readText()
                    val jsonArray = JSONArray(content)
                    val list = mutableListOf<ChatMessage>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            ChatMessage(
                                text = obj.getString("text"),
                                isUser = obj.getBoolean("isUser"),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    _messages.value = list
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // Nếu chưa có lịch sử, thêm câu chào
                duckName.collect { name ->
                    if (_messages.value.isEmpty()) {
                        val intro = ChatMessage("Quạc! Ta là $name. Ta đang đọc dữ liệu điện thoại của ngươi... Hỏi gì thì hỏi đi!", false)
                        _messages.value = listOf(intro)
                        saveChatHistory()
                    }
                }
            }
        }
    }

    private fun saveChatHistory() {
        val currentList = _messages.value
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonArray = JSONArray()
                currentList.forEach { msg ->
                    val obj = JSONObject().apply {
                        put("text", msg.text)
                        put("isUser", msg.isUser)
                        put("timestamp", msg.timestamp)
                    }
                    jsonArray.put(obj)
                }
                chatFile.writeText(jsonArray.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- XỬ LÝ TTS (GIỌNG NÓI) ---
    private fun initTextToSpeech(app: Application) {
        tts = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("vi", "VN"))
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    // --- CẤU HÌNH GIỌNG NHẸ NHÀNG (BÌNH THƯỜNG) ---
                    tts?.setPitch(1.0f) // Giọng trầm ấm, tự nhiên
                    tts?.setSpeechRate(1.0f) // Tốc độ vừa phải, từ tốn
                }
            }
        }
    }

    // Hàm gọi thủ công từ nút bấm
    fun speakMessage(text: String) {
        val cleanText = text.replace("*", "").replace("#", "")
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // --- GỬI TIN NHẮN ---
    fun sendMessage(text: String) {
        val currentList = _messages.value.toMutableList()
        currentList.add(ChatMessage(text, true))
        _messages.value = currentList
        saveChatHistory()
        _isLoading.value = true
        val currentName = duckName.value

        viewModelScope.launch {
            try {
                // GỌI HÀM LẤY TOÀN BỘ DỮ LIỆU (Screen + Tree + Task)
                val fullReport = dataAggregator.getFullDailyReport()

                val history = currentList.dropLast(1).map { (if (it.isUser) "user" else "model") to it.text }

                // Gửi dữ liệu này cho Gemini 2.5 phân tích
                val response = GeminiHelper.chatWithDuck(history, text, currentName, fullReport)

                val newList = _messages.value.toMutableList()
                newList.add(ChatMessage(response, false))
                _messages.value = newList
                saveChatHistory()
            } catch (e: Exception) {
                val errorMsg = "Lỗi kỹ thuật: ${e.message}"
                val newList = _messages.value.toMutableList()
                newList.add(ChatMessage(errorMsg, false))
                _messages.value = newList
                saveChatHistory()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun analyzeMyHabits() {
        sendMessage("Hãy phân tích nghiêm khắc tình hình sử dụng của tôi hôm nay!")
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
    }
}