package com.example.ducktrack.ui.main.ai

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.data.GeminiHelper
import com.example.ducktrack.data.UsageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class AiChatViewModel(application: Application) : AndroidViewModel(application) {

    private val usageRepo = UsageRepository(application)

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("Quạc! Chào bạn, Vịt có thể giúp gì cho việc tập trung hôm nay?", false)
    ))
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun sendMessage(text: String) {
        val currentList = _messages.value.toMutableList()
        currentList.add(ChatMessage(text, true))
        _messages.value = currentList

        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Chuyển lịch sử chat sang định dạng Gemini cần (user/model)
                val history = currentList.dropLast(1).map {
                    (if (it.isUser) "user" else "model") to it.text
                }

                val response = GeminiHelper.chatWithDuck(history, text)

                currentList.add(ChatMessage(response, false))
                _messages.value = currentList
            } catch (e: Exception) {
                currentList.add(ChatMessage("Lỗi kết nối: ${e.message}", false))
                _messages.value = currentList
            } finally {
                _isLoading.value = false
            }
        }
    }

    // TÍNH NĂNG ĐẶC BIỆT: Phân tích thói quen
    fun analyzeMyHabits() {
        val currentList = _messages.value.toMutableList()
        currentList.add(ChatMessage("Hãy phân tích thói quen sử dụng của tôi hôm nay!", true))
        _messages.value = currentList
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // 1. Lấy dữ liệu Usage thực tế
                val end = System.currentTimeMillis()
                val start = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                }.timeInMillis

                // Lấy Top 5 app dùng nhiều nhất
                val stats = usageRepo.getTopAppsStats(start, end)
                val top5 = stats.toList().sortedByDescending { it.second }.take(5)

                // Tạo chuỗi báo cáo để gửi cho AI
                val sb = StringBuilder()
                top5.forEach { (name, ms) ->
                    val minutes = ms / 60000
                    sb.append("- $name: $minutes phút\n")
                }

                if (sb.isEmpty()) sb.append("Chưa có dữ liệu sử dụng đáng kể.")

                // 2. Gửi cho Gemini
                val response = GeminiHelper.analyzeUsageAndSuggest(sb.toString())

                currentList.add(ChatMessage(response, false))
                _messages.value = currentList
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}