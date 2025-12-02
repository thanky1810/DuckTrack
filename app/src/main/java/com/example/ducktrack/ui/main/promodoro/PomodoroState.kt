package com.example.ducktrack.ui.main.promodoro

import com.example.ducktrack.ui.main.garden.SeedType
import com.example.ducktrack.R

enum class BackgroundSound(val displayName: String, val resId: Int?) {
    OFF("Tắt nhạc", null),
    RAIN("Mưa bão ⛈️", R.raw.rain_thunderstorm),
    SNOW("Tuyết rơi ❄️", R.raw.snow_falling_tree),
    SEA("Sóng biển 🌊", R.raw.sea_wave)
}
// 1. Các trạng thái của timer
enum class PomodoroState {
    Ready,
    Running,
    Break,
    Finished,
    Failed
}

// 2. Data class chứa toàn bộ trạng thái cho UI
data class PomodoroUiState(
    val pomodoroState: PomodoroState = PomodoroState.Ready,

    // --- CÁC BIẾN THỜI GIAN ---
    val focusDurationMillis: Long = 25 * 60 * 1000L,
    val breakDurationMillis: Long = 5 * 60 * 1000L,

    // THÊM MỚI: Thời gian nghỉ dài (mặc định 15 phút)
    val longBreakDurationMillis: Long = 15 * 60 * 1000L,

    // THÊM MỚI: Số phiên cần làm để được nghỉ dài (mặc định 4)
    val sessionsBeforeLongBreak: Int = 4,

    // THÊM MỚI: Đếm số phiên đã hoàn thành hiện tại
    val currentSessionCount: Int = 0,

    val remainingTimeMillis: Long = 25 * 60 * 1000L,
    val isTimerRunning: Boolean = false,

    // Kết nối với Model và Repository
    val selectedSeed: SeedType = SeedType.NORMAL,
    val availableSeeds: List<SeedType> = listOf(SeedType.NORMAL),

    // Trạng thái của các Dialog
    val showSettingsDialog: Boolean = false,
    val showFailedDialog: Boolean = false,
    val showHarvestDialog: Boolean = false,

    val selectedSound: BackgroundSound = BackgroundSound.OFF
)