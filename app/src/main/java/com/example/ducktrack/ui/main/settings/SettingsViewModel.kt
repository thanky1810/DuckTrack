package com.example.ducktrack.ui.main.settings

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.data.LimitsStore
import com.example.ducktrack.service.UsageMonitorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val limitsStore = LimitsStore(app)

    // 1. Lấy trạng thái giám sát từ DataStore và chuyển thành State
    //    để Composable có thể lắng nghe
    val isMonitoringEnabled = limitsStore.isMonitoringEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false // Giá trị ban đầu khi app mới mở
        )

    /**
     * Hàm này được gọi khi người dùng gạt nút Switch
     */
    fun setMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch {
            // 2. Lưu trạng thái mới vào DataStore
            limitsStore.setMonitoringEnabled(enabled)

            // 3. Đồng thời, khởi động hoặc dừng Service
            if (enabled) {
                startMonitoringService()
            } else {
                stopMonitoringService()
            }
        }
    }

    // (Đây là 2 hàm logic bạn đã viết, di chuyển từ SettingsScreen vào đây)
    private fun startMonitoringService() {
        val context = getApplication<Application>()
        val intent = Intent(context, UsageMonitorService::class.java).apply {
            action = UsageMonitorService.ACTION_START_MONITORING
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopMonitoringService() {
        val context = getApplication<Application>()
        val intent = Intent(context, UsageMonitorService::class.java).apply {
            action = UsageMonitorService.ACTION_STOP_MONITORING
        }
        context.startService(intent)
    }
}
