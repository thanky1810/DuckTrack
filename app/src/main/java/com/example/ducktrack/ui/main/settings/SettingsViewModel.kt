package com.example.ducktrack.ui.main.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.data.UserPreferences
import com.example.ducktrack.service.OverlayService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val userPrefs = UserPreferences(application)

    // Các State cũ
    val isVibration = userPrefs.isVibrationEnabled.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    val isKeepScreenOn = userPrefs.isKeepScreenOn.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val isMonitoringEnabled = OverlayService.isServiceRunning.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    // --- CÁC STATE MỚI CHO THEME ---
    val isChristmasTheme = userPrefs.isChristmasTheme.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    private val _isThemeChanging = MutableStateFlow(false)
    val isThemeChanging = _isThemeChanging.asStateFlow()
    // --------------------------------

    fun setVibration(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setVibration(enabled) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setKeepScreenOn(enabled) }
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        if (enabled) OverlayService.startService(getApplication())
        else OverlayService.stopService(getApplication())
    }

    // --- HÀM TOGGLE THEME MỚI ---
    fun toggleChristmasTheme(enabled: Boolean) {
        viewModelScope.launch {
            _isThemeChanging.value = true // Bật loading
            delay(1500) // Giả lập loading để người dùng thấy hiệu ứng
            userPrefs.setChristmasTheme(enabled)
            _isThemeChanging.value = false // Tắt loading
        }
    }
}

