package com.example.ducktrack.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig

object GeminiHelper {
    // Thay API KEY của bạn vào đây
    private const val API_KEY = "YOUR_API_KEY_HERE"

    // Cấu hình con Vịt
    private val duckSystemInstruction = """
        Bạn là DuckTrack, một trợ lý ảo hình con vịt thông minh và hài hước.
        Nhiệm vụ của bạn là giúp người dùng cai nghiện điện thoại và tập trung làm việc.
        
        Tính cách của bạn:
        - Luôn xưng hô là "Vịt" hoặc "Tớ" và gọi người dùng là "Bạn".
        - Thỉnh thoảng thêm tiếng "Quạc quạc!" hoặc biểu tượng 🦆 vào câu nói.
        - Hơi đanh đá một chút nếu người dùng lười biếng, nhưng rất động viên khi họ làm tốt.
        - Trả lời ngắn gọn, súc tích, không dài dòng.
    """.trimIndent()

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash", // Bản Flash nhanh và rẻ (free)
        apiKey = API_KEY,
        systemInstruction = content { text(duckSystemInstruction) }
    )

    // Hàm 1: Chat bình thường
    suspend fun chatWithDuck(history: List<Pair<String, String>>, userMessage: String): String {
        val chat = model.startChat(
            history = history.map { (role, msg) ->
                content(role) { text(msg) }
            }
        )
        val response = chat.sendMessage(userMessage)
        return response.text ?: "Quạc? Mạng lag quá, Vịt không nghe rõ!"
    }

    // Hàm 2: Phân tích thói quen và gợi ý lịch trình
    suspend fun analyzeUsageAndSuggest(usageData: String): String {
        val prompt = """
            Đây là dữ liệu sử dụng điện thoại hôm nay của tôi:
            $usageData
            
            Hãy phân tích ngắn gọn xem tôi đang lãng phí thời gian vào đâu.
            Sau đó gợi ý cho tôi một lịch trình tập trung (Pomodoro) cụ thể để khắc phục.
            Hãy trả lời theo phong cách con vịt DuckTrack.
        """.trimIndent()

        val response = model.generateContent(prompt)
        return response.text ?: "Quạc! Vịt không đọc được dữ liệu."
    }
}