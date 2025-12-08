package com.example.ducktrack.ui.main.garden

// Model cho item trong cửa hàng
data class StoreItem(
    val seedType: SeedType,
    val isUnlocked: Boolean
)

// Model cho cây đã trồng để hiển thị lên UI
data class GrownTreeUI(
    val id: String,
    val seedType: SeedType,
    val plantedAt: Long,
    val config: String,
    // THÊM 2 TRƯỜNG NÀY ĐỂ HIỂN THỊ
    val setIndex: Int = 1,
    val sessionIndex: Int = 1
)

// Trạng thái tổng của GardenScreen
data class GardenUiState(
    val userPoints: Int = 0,
    val storeItems: List<StoreItem> = emptyList(),

    // Danh sách cây của ngày đang chọn
    val treesForSelectedDate: List<GrownTreeUI> = emptyList(),

    val dateText: String = "",
    val isToday: Boolean = true,
    val showNotEnoughPointsDialog: Boolean = false
)