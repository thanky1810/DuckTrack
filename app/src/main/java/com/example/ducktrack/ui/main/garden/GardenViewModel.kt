package com.example.ducktrack.ui.main.garden

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.MyApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 1. ViewModel -> AndroidViewModel(application)
class GardenViewModel(application: Application) : AndroidViewModel(application) {

    // 2.  Lấy repository từ Application
    private val repository = (application as MyApplication).repository

    private val _uiState = MutableStateFlow(GardenUiState())
    val uiState: StateFlow<GardenUiState> = _uiState.asStateFlow()

    init {
        // 3. Cách collect Flow từ repository
        combine(
            repository.userPoints,
            repository.unlockedSeeds,
            repository.grownTrees
        ) { points, unlocked, grown ->

            // 1. Tạo danh sách cửa hàng
            val storeItems = SeedType.values().map { seed ->
                StoreItem(
                    seedType = seed,
                    isUnlocked = seed in unlocked
                )
            }

            // 2. Tạo danh sách mảnh đất (12 ô)
            val plots = List(12) { index ->
                grown.getOrNull(index) // Lấy cây, nếu không có thì là null
            }

            GardenUiState(
                userPoints = points,
                storeItems = storeItems,
                gardenPlots = plots
            )
        }
            .onEach { newState ->
                _uiState.value = newState // Cập nhật state
            }
            .launchIn(viewModelScope) // Tự động chạy và hủy
    }

    // 4. onUnlockSeed phải dùng viewModelScope
    fun onUnlockSeed(seed: SeedType) {
        viewModelScope.launch {
            val success = repository.unlockSeed(seed)
            if (!success) {
                _uiState.update {
                    it.copy(showNotEnoughPointsDialog = true)
                }
            }
        }
    }

    //  Dùng để tắt dialog khi người dùng nhấn "Đã hiểu"
    fun onDismissNotEnoughPointsDialog() {
        _uiState.update {
            it.copy(showNotEnoughPointsDialog = false)
        }
    }
}