package com.example.ducktrack.data

import android.util.Log
import com.example.ducktrack.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiHelper {
    private const val modelName = "gemini-2.5-flash"
    private const val API_KEY = BuildConfig.GEMINI_API_KEY

    private val generativeModel = GenerativeModel(
        modelName = modelName,
        apiKey = API_KEY,
        generationConfig = generationConfig {
            temperature = 0.6f
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
            
            QUY TẮC ĐỊNH DẠNG VĂN BẢN (BẮT BUỘC):
            1. Khi nhắc đến Tên Ứng Dụng (dựa vào dữ liệu), BẮT BUỘC bọc nó trong 2 dấu gạch dưới. Ví dụ: __Facebook__, __TikTok__.
            2. Khi nhắc đến Thời lượng/Thời gian, BẮT BUỘC bọc nó trong 2 dấu sao. Ví dụ: **2 giờ 30 phút**, **15 phút**.
            
            TÍNH CÁCH & GIỌNG ĐIỆU:
            - Nhẹ nhàng, thấu hiểu, không phán xét gay gắt.
            - Phân tích dựa trên SỐ LIỆU: Luôn kết nối giữa việc "dùng điện thoại nhiều" với "nhiệm vụ chưa xong" hoặc "số cây trồng được".
            - Kết thúc câu bằng tiếng "Quạc~" nhẹ nhàng.
            
            VÍ DỤ TRẢ LỜI CHUẨN:
            "Mình thấy bạn lướt __Facebook__ và __TikTok__ mất tận **4 tiếng** mà mới trồng được **1 cái cây** thôi. Mấy nhiệm vụ kia vẫn đang đợi kìa. Quạc~"
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

                val systemInstruction = content("user") {
                    text(getSystemPrompt(duckName, currentData))
                }

                val chatHistory = history.map { (role, text) ->
                    content(role) { text(text) }
                }.toMutableList()


                chatHistory.add(0, systemInstruction)

                val chat = generativeModel.startChat(chatHistory)
                val response = chat.sendMessage(message)

                response.text ?: "Dữ liệu đang được xử lý... Quạc~"
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: ""
                Log.e("GeminiError", "Lỗi chi tiết: $errorMsg")


                when {
                    errorMsg.contains("503") -> {
                        "Đầu mình đang quá tải thông tin mất rồi. Bạn đợi 1 phút rồi hỏi lại nhé! Quạc~"
                    }

                    errorMsg.contains("Quota") || errorMsg.contains("exceeded") -> {
                        "Úi, mình xài hết số lần suy nghĩ miễn phí rồi. Bạn kiểm tra lại API Key nhé! Quạc~"
                    }

                    errorMsg.contains("403") -> {
                        "API Key của bạn bị lỗi hoặc chưa cấp quyền rồi. Kiểm tra lại nha! Quạc~"
                    }

                    else -> {
                        "Có chút trục trặc kết nối... Bạn kiểm tra mạng giúp mình nha! Quạc~"
                    }
                }
            }
        }
    }
}