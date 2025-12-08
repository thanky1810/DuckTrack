package com.example.ducktrack.ui.main.garden

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.MyApplication
import com.example.ducktrack.data.model.GrownTree
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class GardenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MyApplication).repository

    private val _uiState = MutableStateFlow(GardenUiState())
    val uiState: StateFlow<GardenUiState> = _uiState.asStateFlow()

    // Biến lưu ngày đang chọn (Timestamp)
    private val _selectedDateMs = MutableStateFlow(System.currentTimeMillis())

    init {
        // Kết hợp 4 luồng dữ liệu:
        // 1. Điểm số
        // 2. Cây trồng từ Cloud
        // 3. Ngày đang chọn
        // 4. Danh sách hạt giống đã mở khóa (TỪ LOCAL DB)
        combine(
            repository.userPoints,
            repository.getGrownTreesStream(),
            _selectedDateMs,
            repository.unlockedSeeds // <--- THÊM LUỒNG NÀY
        ) { points, allTreesFromCloud, selectedDate, unlockedSeedsSet ->

            // 1. LOGIC LỌC CÂY ĐÃ TRỒNG THEO NGÀY
            val startOfDay = getStartOfDay(selectedDate)
            val endOfDay = getEndOfDay(selectedDate)

            val treesToday = allTreesFromCloud.filter {
                it.plantedAt in startOfDay..endOfDay
            }.map { treeFirestore ->
                val type = SeedType.values().find { it.id == treeFirestore.seedId } ?: SeedType.NORMAL
                GrownTreeUI(
                    id = treeFirestore.id,
                    seedType = type,
                    plantedAt = treeFirestore.plantedAt,
                    config = treeFirestore.config,
                    // Map dữ liệu từ Firestore sang UI
                    setIndex = treeFirestore.sessionSetIndex,
                    sessionIndex = treeFirestore.sessionIndexInSet
                )
            }

            // 2. TẠO DANH SÁCH CỬA HÀNG (CHECK KHÓA/MỞ TỪ DB)
            val storeItems = SeedType.values().map { seed ->
                // Kiểm tra xem seed này có trong danh sách đã mở khóa không
                // Seed NORMAL luôn luôn mở
                val isUnlocked = (seed == SeedType.NORMAL) || unlockedSeedsSet.contains(seed)

                StoreItem(seed, isUnlocked)
            }

            // 3. CẬP NHẬT UI
            val dateText = formatDate(selectedDate)
            val isToday = isSameDay(selectedDate, System.currentTimeMillis())

            _uiState.value.copy(
                userPoints = points,
                storeItems = storeItems, // List này giờ đã phản ánh đúng trạng thái mua/chưa mua
                treesForSelectedDate = treesToday,
                dateText = dateText,
                isToday = isToday
            )
        }.onEach { newState ->
            _uiState.value = newState
        }.launchIn(viewModelScope)
    }

    // --- CÁC HÀM XỬ LÝ SỰ KIỆN TỪ UI ---

    fun onUnlockSeed(seed: SeedType) {
        viewModelScope.launch {
            // Gọi repository để trừ điểm và mở khóa (Lưu vào Room)
            val success = repository.unlockSeed(seed)
            if (!success) {
                _uiState.update { it.copy(showNotEnoughPointsDialog = true) }
            }
        }
    }

    fun onDismissNotEnoughPointsDialog() {
        _uiState.update { it.copy(showNotEnoughPointsDialog = false) }
    }

    // --- CÁC HÀM ĐỔI NGÀY ---

    fun previousDay() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = _selectedDateMs.value
        cal.add(Calendar.DAY_OF_YEAR, -1)
        _selectedDateMs.value = cal.timeInMillis
    }

    fun nextDay() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = _selectedDateMs.value
        if (!isSameDay(cal.timeInMillis, System.currentTimeMillis())) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            _selectedDateMs.value = cal.timeInMillis
        }
    }

    // --- HELPER NGÀY GIỜ ---

    private fun getStartOfDay(time: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = time }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfDay(time: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = time }
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatDate(time: Long): String {
        val date = java.util.Date(time)
        val fmt = java.text.SimpleDateFormat("dd 'tháng' MM", java.util.Locale("vi", "VN"))
        return if (isSameDay(time, System.currentTimeMillis())) "Hôm nay, ${fmt.format(date)}" else fmt.format(date)
    }
}