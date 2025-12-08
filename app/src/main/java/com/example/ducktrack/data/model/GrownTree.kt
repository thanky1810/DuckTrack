package com.example.ducktrack.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class GrownTree(
    @DocumentId
    val id: String = "",

    val seedId: String = "normal",
    val plantedAt: Long = 0L,
    val config: String = "25/5/4/15",

    // --- THÊM MỚI ---
    // Số thứ tự của bộ phiên trong ngày (Bộ 1, Bộ 2...)
    @get:PropertyName("sessionSetIndex")
    @set:PropertyName("sessionSetIndex")
    var sessionSetIndex: Int = 1,

    // Thứ tự của phiên trong bộ đó (Phiên 1/4, 2/4...)
    @get:PropertyName("sessionIndexInSet")
    @set:PropertyName("sessionIndexInSet")
    var sessionIndexInSet: Int = 1
)