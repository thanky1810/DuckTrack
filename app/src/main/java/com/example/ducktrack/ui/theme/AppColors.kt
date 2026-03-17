package com.example.ducktrack.ui.theme

import androidx.compose.ui.graphics.Color

object AppColors {
    // Nền chính: Màu kem nhẹ/trắng sứ
    val BackgroundWhite = Color(0xFFF9F9F9)

    // Màu chủ đạo: Xanh lá đậm
    val ButtonGreen = Color(0xFF2E7D32)

    // Màu text chính
    val TextGreen = Color(0xFF1B5E20)

    // Màu phụ/Nền phụ
    val SurfaceColor = Color(0xFFFFFFFF)

    // Màu xám
    val ButtonGray = Color(0xFFF0F0F0)
    val TextGray = Color(0xFF757575)

    // Palette cho biểu đồ
    val chartPalette = listOf(
        Color(0xFF43A047),
        Color(0xFF1E88E5),
        Color(0xFFFB8C00),
        Color(0xFFD81B60),
        Color(0xFF8E24AA),
        Color(0xFF00ACC1),
        Color(0xFF7CB342),
        Color(0xFF6D4C41)
    )

    fun getColorForIndex(index: Int): Color {
        return chartPalette[index % chartPalette.size]
    }
}