package com.example.ducktrack.ui.main.promodoro

// Định nghĩa các trạng thái của Pomodoro
enum class PomodoroState {
    Ready,
    Running,
    Break,
    Finished,
    Failed
}

//  Data class chứa toàn bộ trạng thái cho UI
data class PomodoroUiState(
    val pomodoroState: PomodoroState = PomodoroState.Ready,
    val focusDurationMillis: Long = 25 * 60 * 1000L,
    val breakDurationMillis: Long = 5 * 60 * 1000L,
    val remainingTimeMillis: Long = 25 * 60 * 1000L,
    val isTimerRunning: Boolean = false,
    val selectedSeed: String = "Cây thường",

    // Trạng thái của các Dialog
    val showSettingsDialog: Boolean = false,
    val showFailedDialog: Boolean = false,
    val showHarvestDialog: Boolean = false
)