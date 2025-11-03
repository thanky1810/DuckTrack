package com.example.ducktrack.data.model

data class AppUsage(
    val packageName: String,          // khóa logic, ở đây chính là label chuẩn hoá (vd "Facebook")
    val label: String,                // tên hiển thị
    val totalForegroundMs: Long,
    val iconPackage: String? = null   // gói đại diện để lấy icon (vd com.facebook.katana)
)
