package com.example.ducktrack.ui.theme

import androidx.compose.ui.graphics.Color

// Màu sắc dựa trên ảnh 3-panel mới (image_3a1c60.jpg)
object AppColors {
    // Nền trắng cho cả 3 màn hình
    val BackgroundWhite = Color(0xFFFFFFFF)

    // Màu xanh lá đậm cho text và các nút chính
    val TextGreen = Color(0xFF004D40) // (Một màu xanh lá cây đậm)
    val ButtonGreen = Color(0xFF2E7D32) // (Xanh lá cây cho nút)

    // Màu xám cho nút "Hủy"
    val ButtonGray = Color(0xFFE0E0E0) // Nền xám nhạt
    val TextGray = Color(0xFF616161)   // Chữ xám đậm

    // ========== Palette cho PieChart và AppUsageRow ==========
    /**
     * Bảng màu chung cho PieChart và AppUsageRow
     * Đảm bảo màu đồng nhất giữa biểu đồ và danh sách app
     */
    val chartPalette = listOf(
        Color(0xFF4CAF50), // Xanh lá
        Color(0xFF2196F3), // Xanh dương
        Color(0xFFFF9800), // Cam
        Color(0xFFE91E63), // Hồng
        Color(0xFF9C27B0), // Tím
        Color(0xFF00BCD4), // Xanh cyan
        Color(0xFF8BC34A), // Xanh lá nhạt
        Color(0xFF795548)  // Nâu
    )

    /**
     * Lấy màu theo index, tự động lặp lại nếu vượt quá số màu
     */
    fun getColorForIndex(index: Int): Color {
        return chartPalette[index % chartPalette.size]
    }
}