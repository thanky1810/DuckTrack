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

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = UsageRepository(app)
    private val limitsStore = LimitsStore(app)

    // Các state mà DashboardScreen đang đọc
    var usages by mutableStateOf<List<AppUsage>>(emptyList())
        private set

    var totalMs by mutableStateOf(0L)
        private set

    var limits by mutableStateOf<Map<String, Int>>(emptyMap())
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

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repo.queryToday()
            val total = list.sumOf { it.totalForegroundMs }
            val allLimits = limitsStore.getAll()

            withContext(Dispatchers.Main) {
                usages = list
                totalMs = total
                limits = allLimits
            }
        }
    }

    /**
     * Set giới hạn cho 1 app:
     *  - pkg: packageName thật (vd: com.ss.android.ugc.trill)
     *  - minutes: số phút giới hạn
     *  - baselineMs: tổng ms đã dùng tới thời điểm set
     */
    fun setLimit(pkg: String, minutes: Int, baselineMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Lưu giới hạn + baseline
            limitsStore.setLimitWithBaseline(pkg, minutes, baselineMs)
            val m = limitsStore.getAll()

            withContext(Dispatchers.Main) {
                limits = m

                // 2. Nếu đã có quyền overlay, khởi động service giám sát
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

    /**
     * Khởi động UsageMonitorService
     */
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
        } catch (_: Exception) {
            // Service đã chạy hoặc lỗi khác -> bỏ qua
        }
    }
}
