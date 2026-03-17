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
    // --- CÁC CÂY CŨ (GIỮ NGUYÊN) ---
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
    ),

    // --- 5 CÂY MỚI THÊM VÀO ---
    TOMATO(
        id = "plant_tomato",
        displayName = "Cây cà chua",
        cost = 200,
        storeIcon = R.drawable.plant_tomato,
        selectionIcon = R.drawable.plant_tomato,
        grownIcon = R.drawable.plant_tomato
    ),
    TOBACCO(
        id = "plant_tobacco",
        displayName = "Cây thuốc lá",
        cost = 750,
        storeIcon = R.drawable.plant_tobacco,
        selectionIcon = R.drawable.plant_tobacco,
        grownIcon = R.drawable.plant_tobacco
    ),
    RED_POPPY(
        id = "flower_red_poppy",
        displayName = "Cây anh túc",
        cost = 1000,
        storeIcon = R.drawable.flower_red_poppy,
        selectionIcon = R.drawable.flower_red_poppy,
        grownIcon = R.drawable.flower_red_poppy
    ),
    CHRISTMAS_TREE(
        id = "tree_christmas",
        displayName = "Cây thông cao cấp",
        cost = 1750,
        storeIcon = R.drawable.tree_christmas,
        selectionIcon = R.drawable.tree_christmas,
        grownIcon = R.drawable.tree_christmas
    ),
    CHERRY_BLOSSOM(
        id = "tree_cherry",
        displayName = "Cây anh đào",
        cost = 2000,
        storeIcon = R.drawable.tree_cherry,
        selectionIcon = R.drawable.tree_cherry,
        grownIcon = R.drawable.tree_cherry
    );

    companion object {
        fun fromId(id: String?): SeedType? = entries.find { it.id == id }
    }
}