package com.example.ducktrack.ui.main.tasks

data class TaskStats(
    val total: Int = 0,
    val completed: Int = 0,
    val pending: Int = 0,
    // --- THÊM MỚI: Chi tiết 4 thẻ ---
    val details: List<QuadrantStat> = emptyList()
)

data class QuadrantStat(
    val name: String, // Tên thẻ (VD: Quan trọng & Khẩn cấp)
    val total: Int,
    val completed: Int,
    val pending: Int
)