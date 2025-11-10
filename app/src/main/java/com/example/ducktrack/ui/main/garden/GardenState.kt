package com.example.ducktrack.ui.main.garden

// Đại diện cho một món hàng trong cửa hàng
data class StoreItem(
    val seedType: SeedType,
    val isUnlocked: Boolean
)

// Trạng thái tổng của GardenScreen
data class GardenUiState(
    val userPoints: Int = 0,
    val storeItems: List<StoreItem> = emptyList(),
    // Danh sách 12 ô, null nghĩa là ô trống
    val gardenPlots: List<SeedType?> = List(12) { null },
    val showNotEnoughPointsDialog: Boolean = false
)