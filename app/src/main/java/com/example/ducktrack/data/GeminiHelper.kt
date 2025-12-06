package com.example.ducktrack.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiHelper {
    // 1. Dán Key MỚI TẠO vào đây
    private const val API_KEY = "DÁN_KEY_MỚI_VÀO_ĐÂY"

    // 2. Dùng model này để tương thích tốt nhất với SDK 0.9.0
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash-latest",
        apiKey = API_KEY
    )

    suspend fun chatWithDuck(history: List<Pair<String, String>>, userMessage: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val chat = model.startChat(
                    history = history.map { (role, msg) ->
                        content(role) { text(msg) }
                    }
                )
                // Nhắc tính cách Vịt ở mỗi tin nhắn để AI nhớ
                val prompt = "Bạn là trợ lý Vịt DuckTrack hài hước. Hãy trả lời ngắn gọn: $userMessage"

                val response = chat.sendMessage(prompt)
                response.text ?: "Quạc? Mạng lag quá!"
            } catch (e: Exception) {
                e.printStackTrace()
                // Bắt lỗi MissingFieldException để không crash app
                if (e.message?.contains("MissingFieldException") == true || e.message?.contains("404") == true) {
                    "Quạc! Lỗi kết nối API (404). Vui lòng kiểm tra lại API Key."
                } else {
                    "Quạc! Có lỗi xảy ra: ${e.localizedMessage}"
                }
            }
        }
    }

    suspend fun analyzeUsageAndSuggest(usageData: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Đóng vai trợ lý Vịt DuckTrack (hài hước, hay nói Quạc).
                    Dữ liệu sử dụng điện thoại:
                    $usageData
                    Hãy phân tích ngắn gọn và gợi ý lịch trình Pomodoro.
                """.trimIndent()

                val response = model.generateContent(prompt)
                response.text ?: "Quạc! Không đọc được dữ liệu."
            } catch (e: Exception) {
                e.printStackTrace()
                if (e.message?.contains("404") == true) {
                    "Quạc! Lỗi API Key (404). Hãy tạo Key mới trong Project mới."
                } else {
                    "Quạc! Lỗi phân tích: ${e.localizedMessage}"
                }
            }
        }
    }
}