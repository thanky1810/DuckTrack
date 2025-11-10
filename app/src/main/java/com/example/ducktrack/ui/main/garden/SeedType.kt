package com.example.ducktrack.ui.main.garden

import com.example.ducktrack.R

// Định nghĩa tất cả các loại cây trong game
enum class SeedType(
    val id: String,
    val displayName: String,
    val cost: Int,
    val storeIcon: Int, // Icon trong cửa hàng
    val selectionIcon: Int, // Icon khi chọn ở Pomodoro
    val grownIcon: Int // Icon khi mọc trên mảnh đất
) {
    NORMAL(
        id = "normal",
        displayName = "Cây thường",
        cost = 0,
        storeIcon = R.drawable.tree_normal,
        selectionIcon = R.drawable.tree_normal,
        grownIcon = R.drawable.tree_normal
    ),
    PINE(
        id = "pine",
        displayName = "Cây thông",
        cost = 150, // 150 điểm 🌟
        storeIcon = R.drawable.tree_pine,
        selectionIcon = R.drawable.tree_pine,
        grownIcon = R.drawable.tree_pine
    ),
    RED_LEAF(
        id = "red_leaf",
        displayName = "Cây lá đỏ",
        cost = 350, // 350 điểm 🌟
        storeIcon = R.drawable.tree_red_leaf,
        selectionIcon = R.drawable.tree_red_leaf,
        grownIcon = R.drawable.tree_red_leaf
    );


    companion object {
        fun fromId(id: String?): SeedType? = values().find { it.id == id }
    }
}