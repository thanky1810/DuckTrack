package com.example.ducktrack.ui.main.pomodoro

import com.example.ducktrack.ui.main.garden.SeedType
import com.example.ducktrack.R

enum class BackgroundSound(val displayName: String, val resId: Int?) {
    OFF("Tắt nhạc", null),
    RAIN("Mưa bão ⛈️", R.raw.rain_thunderstorm),
    SNOW("Tuyết rơi ❄️", R.raw.snow_falling_tree),
    SEA("Sóng biển 🌊", R.raw.sea_wave)
}

enum class PomodoroState {
    Ready,
    Running,
    Break,
    Finished,
    Failed
}

data class PomodoroUiState(
    val pomodoroState: PomodoroState = PomodoroState.Ready,

    val focusDurationMillis: Long = 25 * 60 * 1000L,
    val breakDurationMillis: Long = 5 * 60 * 1000L,
    val longBreakDurationMillis: Long = 15 * 60 * 1000L,
    val sessionsBeforeLongBreak: Int = 4,
    val currentSessionCount: Int = 0,
    val remainingTimeMillis: Long = 25 * 60 * 1000L,
    val isTimerRunning: Boolean = false,

    val selectedSeed: SeedType = SeedType.NORMAL,
    val availableSeeds: List<SeedType> = listOf(SeedType.NORMAL),

    val showSettingsDialog: Boolean = false,
    val showFailedDialog: Boolean = false,
    val showHarvestDialog: Boolean = false,

    // --- THÊM MỚI: State cho tính năng chọn Tag ---
    val showTagSelectionDialog: Boolean = false,
    val currentTag: String = "Học tập",
    // ---------------------------------------------

    val selectedSound: BackgroundSound = BackgroundSound.OFF,
    val isVibrationEnabled: Boolean = true,
    val isKeepScreenOn: Boolean = false
)