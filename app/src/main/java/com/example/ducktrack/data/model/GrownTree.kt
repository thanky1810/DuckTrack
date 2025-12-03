package com.example.ducktrack.data.model

import com.google.firebase.firestore.DocumentId

data class GrownTree(
    // @DocumentId giúp Firestore tự điền ID của document vào biến này
    @DocumentId
    val id: String = "",

    // Lưu ID của loại hạt giống (vd: "pine", "normal")
    val seedId: String = "normal",

    // Thời gian trồng xong (tính bằng mili giây)
    val plantedAt: Long = 0L,

    // THÊM MỚI: Lưu cấu hình phiên tập trung (vd: "25/5/4/15")
    val config: String = "25/5/4/15"
)