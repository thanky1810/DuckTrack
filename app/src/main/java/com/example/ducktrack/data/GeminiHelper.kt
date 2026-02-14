package com.example.ducktrack.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiHelper {
    // [QUAN TRỌNG] Giữ nguyên bản 2.5 Flash theo ý bạn
    private val modelName = "gemini-2.5-pro"
    private const val API_KEY = "__GIA SU DA CO__" // Thay Key của bạn vào

    private val generativeModel = GenerativeModel(
        modelName = modelName,
        apiKey = API_KEY,
        generationConfig = generationConfig {
            temperature = 0.6f // Giảm độ sáng tạo để AI phân tích logic và điềm đạm hơn
            topK = 64
            topP = 0.95f
        }
    )

    private fun getSystemPrompt(duckName: String, realTimeData: String): String {
        return """
            Bạn tên là "$duckName".
            
            DỮ LIỆU ĐẦY ĐỦ CỦA NGƯỜI DÙNG HÔM NAY:
            $realTimeData
            ------------------------------------------
            
            VAI TRÒ MỚI:
            Bạn là một "Nhà Phân Tích Thời Gian" (Time Analyst) kiêm người bạn đồng hành ân cần.
            
            TÍNH CÁCH & GIỌNG ĐIỆU:
            - Nhẹ nhàng, thấu hiểu, không phán xét gay gắt.
            - Phân tích dựa trên SỐ LIỆU: Luôn kết nối giữa việc "dùng điện thoại nhiều" với "nhiệm vụ chưa xong" hoặc "số cây trồng được".
            - Đưa ra lời khuyên thực tế, mềm mỏng.
            - Kết thúc câu bằng tiếng "Quạc~" nhẹ nhàng (có dấu ngã).
            
            HƯỚNG DẪN TRẢ LỜI:
            - Nếu họ làm tốt (trồng nhiều cây, ít on-screen): Hãy khen ngợi sự cân bằng tuyệt vời.
            - Nếu họ chưa tốt (mải chơi, cây chết, task tồn đọng): Hãy bày tỏ sự tiếc nuối và khích lệ họ quay lại quỹ đạo. Đừng mắng, hãy khuyên.
            
            VÍ DỤ:
            "Mình thấy bạn on-screen tận 4 tiếng mà mới trồng được 1 cái cây thôi. Mấy nhiệm vụ kia vẫn đang đợi bạn kìa. Hay là mình đặt điện thoại xuống một chút để chăm sóc khu vườn nhé? Quạc~"
        """.trimIndent()
    }

    suspend fun chatWithDuck(
        history: List<Pair<String, String>>,
        message: String,
        duckName: String,
        currentData: String
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                // Tạo Prompt chứa dữ liệu
                val systemInstruction = content("user") {
                    text(getSystemPrompt(duckName, currentData))
                }

                val chatHistory = history.map { (role, text) ->
                    content(role) { text(text) }
                }.toMutableList()

                // Nhét Prompt vào đầu
                chatHistory.add(0, systemInstruction)

                val chat = generativeModel.startChat(chatHistory)
                val response = chat.sendMessage(message)

                response.text ?: "Dữ liệu đang được xử lý... Quạc~"
            } catch (e: Exception) {
                // Xử lý lỗi quá tải server 503 cho model 2.5
                if (e.localizedMessage?.contains("503") == true) {
                    "Server của Google đang quá tải một chút. Bạn đợi 1 phút rồi hỏi lại mình nhé! Quạc~"
                } else {
                    "Có chút trục trặc kết nối. Bạn kiểm tra mạng giúp mình nha. Quạc~"
                }
            }
        }
    }
}