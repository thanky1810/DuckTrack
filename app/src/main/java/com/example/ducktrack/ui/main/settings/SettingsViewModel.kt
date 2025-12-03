// FILE: SettingsViewModel.kt
package com.example.ducktrack.ui.main.settings

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.data.LimitsStore
import com.example.ducktrack.data.UserPreferences
import com.example.ducktrack.service.UsageMonitorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val limitsStore = LimitsStore(app)
    private val userPrefs = UserPreferences(app)

    val isMonitoringEnabled = limitsStore.isMonitoringEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Đã xóa isDarkMode state

    val isVibration = userPrefs.isVibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isKeepScreenOn = userPrefs.isKeepScreenOn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch {
            limitsStore.setMonitoringEnabled(enabled)
            if (enabled) startMonitoringService() else stopMonitoringService()
        }
    }

    // Đã xóa setDarkMode function

    fun setVibration(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setVibration(enabled) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setKeepScreenOn(enabled) }
    }

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