package com.example.ducktrack.ui.main

import android.app.Application
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ducktrack.data.LimitsStore
import com.example.ducktrack.data.UsageRepository
import com.example.ducktrack.data.model.AppUsage
import com.example.ducktrack.service.UsageMonitorService
import com.example.ducktrack.utils.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = UsageRepository(app)
    private val limitsStore = LimitsStore(app)

    var usages by mutableStateOf<List<AppUsage>>(emptyList())
        private set

    var totalMs by mutableStateOf(0L)
        private set

    var limits by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    // --- THÊM MỚI: QUẢN LÝ NGÀY ĐANG XEM ---
    // Mặc định là ngày hôm nay (dạng Long millis)
    var selectedDateMs by mutableStateOf(System.currentTimeMillis())
        private set

    private val iconCache = HashMap<String, Drawable?>()

    fun iconFor(usage: AppUsage): Drawable? {
        val key = usage.iconPackage ?: return null
        return iconCache[key] ?: run {
            val d = try {
                getApplication<Application>().packageManager.getApplicationIcon(key)
            } catch (_: Exception) {
                null
            }
            iconCache[key] = d
            d
        }
    }

    // Hàm load dữ liệu theo ngày đang chọn
    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            // Gọi hàm mới queryUsageForDate thay vì queryToday
            val list = repo.queryUsageForDate(selectedDateMs)
            val total = list.sumOf { it.totalForegroundMs }
            val allLimits = limitsStore.getAll()

            withContext(Dispatchers.Main) {
                usages = list
                totalMs = total
                limits = allLimits
            }
        }
    }

    // --- CÁC HÀM ĐỔI NGÀY ---

    fun previousDay() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = selectedDateMs
        cal.add(Calendar.DAY_OF_YEAR, -1) // Trừ 1 ngày
        selectedDateMs = cal.timeInMillis
        load() // Load lại dữ liệu
    }

    fun nextDay() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = selectedDateMs
        // Không cho phép đi quá ngày hôm nay (tương lai chưa có dữ liệu)
        if (isToday(cal.timeInMillis)) return

        cal.add(Calendar.DAY_OF_YEAR, 1) // Cộng 1 ngày
        selectedDateMs = cal.timeInMillis
        load()
    }

    // Kiểm tra xem mốc thời gian có phải là hôm nay không
    fun isToday(dateMs: Long): Boolean {
        val cal1 = Calendar.getInstance()
        cal1.timeInMillis = dateMs

        val cal2 = Calendar.getInstance() // Thời gian hiện tại

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    // ... (Giữ nguyên các hàm setLimit, removeLimit, startMonitoringService cũ) ...
    fun setLimit(pkg: String, minutes: Int, baselineMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            limitsStore.setLimitWithBaseline(pkg, minutes, baselineMs)
            val m = limitsStore.getAll()
            withContext(Dispatchers.Main) {
                limits = m
                if (PermissionHelper.hasOverlayPermission(getApplication())) {
                    startMonitoringService()
                }
            }
        }
    }

    fun removeLimit(pkg: String) {
        viewModelScope.launch(Dispatchers.IO) {
            limitsStore.removeLimit(pkg)
            val m = limitsStore.getAll()
            withContext(Dispatchers.Main) {
                limits = m
            }
        }
    }

    private fun startMonitoringService() {
        val context = getApplication<Application>()
        val intent = Intent(context, UsageMonitorService::class.java).apply {
            action = UsageMonitorService.ACTION_START_MONITORING
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {}
    }

    fun getDateText(): String {
        val date = java.util.Date(selectedDateMs)
        val today = java.util.Date()
        val fmt = java.text.SimpleDateFormat("dd 'tháng' MM", java.util.Locale("vi", "VN"))
        val fmtCheck = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)

        val isToday = fmtCheck.format(date) == fmtCheck.format(today)
        return if (isToday) "Hôm nay, ${fmt.format(date)}" else fmt.format(date)
    }
}