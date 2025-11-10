package com.example.ducktrack.ui.main.promodoro

import com.example.ducktrack.ui.main.garden.SeedType


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
    val focusDurationMillis: Long = 25 * 60 * 1000L,
    val breakDurationMillis: Long = 5 * 60 * 1000L,
    val remainingTimeMillis: Long = 25 * 60 * 1000L,
    val isTimerRunning: Boolean = false,

    // Kết nối với Model và Repository
    val selectedSeed: SeedType = SeedType.NORMAL,
    val availableSeeds: List<SeedType> = listOf(SeedType.NORMAL),

    // Trạng thái của các Dialog
    val showSettingsDialog: Boolean = false,
    val showFailedDialog: Boolean = false,
    val showHarvestDialog: Boolean = false
)