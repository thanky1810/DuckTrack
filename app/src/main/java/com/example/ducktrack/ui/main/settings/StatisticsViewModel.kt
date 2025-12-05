package com.example.ducktrack.ui.main.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.data.UsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

// Model cho Danh sách chi tiết
data class AppUsageInfo(
    val name: String,
    val timeMs: Long,
    val timeDisplay: String
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = UsageRepository(application)
    private val pm = application.packageManager

    // Map<Tên hiển thị, Số giờ>
    private val _last7DaysData = MutableStateFlow<Map<String, Float>>(emptyMap())
    val last7DaysData = _last7DaysData.asStateFlow()

    private val _allTimeData = MutableStateFlow<Map<String, Float>>(emptyMap())
    val allTimeData = _allTimeData.asStateFlow()

    private val _topAppsChartData = MutableStateFlow<Map<String, Float>>(emptyMap())
    val topAppsChartData = _topAppsChartData.asStateFlow()

    private val _topAppsList = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val topAppsList = _topAppsList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()

            // 1. Thống kê 7 ngày
            val map7 = LinkedHashMap<String, Float>()
            for (i in 6 downTo 0) {
                cal.timeInMillis = System.currentTimeMillis()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val dateMs = cal.timeInMillis
                val day = cal.get(Calendar.DAY_OF_MONTH); val month = cal.get(Calendar.MONTH) + 1
                val key = "$day/$month"
                val apps = repo.queryUsageForDate(dateMs)
                map7[key] = apps.sumOf { it.totalForegroundMs } / (1000f * 60 * 60)
            }
            _last7DaysData.value = map7

            // 2. Thống kê Toàn bộ (12 tháng)
            cal.timeInMillis = now
            cal.add(Calendar.YEAR, -1)
            val startYear = cal.timeInMillis
            val rawYear = repo.getMonthlyStats(startYear, now)
            _allTimeData.value = rawYear.mapValues { it.value / (1000f * 60 * 60) }

            // 3. TOP 10 APP
            // Lấy danh sách Top 10 từ Repo
            val rawTop10Map = repo.getTopAppsStats(startYear, now)
            val sortedTop10 = rawTop10Map.toList()
                .sortedByDescending { it.second }
                .take(10)

            // A. Data cho Biểu đồ (QUAN TRỌNG: Lấy tên gốc, KHÔNG CẮT NGẮN)
            val chartMap = LinkedHashMap<String, Float>()
            sortedTop10.forEach { (label, ms) ->
                // Truyền nguyên tên đầy đủ để khi Zoom lên sẽ thấy hết
                chartMap[label] = ms / (1000f * 60 * 60)
            }
            _topAppsChartData.value = chartMap

            // B. Data cho Danh sách chi tiết
            val listData = sortedTop10.map { (label, ms) ->
                val hours = ms / (1000f * 60 * 60)
                AppUsageInfo(
                    name = label,
                    timeMs = ms,
                    timeDisplay = String.format("%.1f giờ", hours)
                )
            }
            _topAppsList.value = listData

            _isLoading.value = false
        }
    }
}