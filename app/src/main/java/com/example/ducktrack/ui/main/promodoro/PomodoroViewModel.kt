package com.example.ducktrack.ui.main.promodoro
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PomodoroViewModel : ViewModel() {

    // --- 1. State ---
    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    // --- 2. Các hàm xử lý sự kiện (Events) từ UI ---

    /**
     * Xử lý khi người dùng nhấn nút chính (Sẵn sàng / Dừng lại / Nghỉ ngơi)
     */
    fun onMainButtonClick() {
        val currentState = _uiState.value
        when (currentState.pomodoroState) {
            PomodoroState.Running -> {
                // Đang chạy -> Dừng lại (Failed)
                stopTimer(isFailed = true)
            }
            PomodoroState.Ready, PomodoroState.Finished, PomodoroState.Failed -> {
                // Đang Ready/Finished/Failed -> Bắt đầu (Running)
                startTimer()
            }
            PomodoroState.Break -> {
                // Đang nghỉ ngơi -> Bỏ qua (Skip) và về Ready
                skipBreak()
            }
        }
    }

    /**
     * Xử lý khi chọn hạt giống
     */
    fun onSeedSelected(seedName: String) {
        _uiState.update { it.copy(selectedSeed = seedName) }
    }

    /**
     * Xử lý khi nhấn nút "Thu hoạch"
     */
    fun onHarvestClick() {
        _uiState.update { it.copy(showHarvestDialog = true) }
    }

    /**
     * Đóng dialog Thu hoạch và TỰ ĐỘNG BẮT ĐẦU NGHỈ NGƠI
     */
    fun onDismissHarvestDialog() {
        // CHỈNH SỬA: Thay vì về Ready, chúng ta bắt đầu nghỉ ngơi
        _uiState.update { it.copy(showHarvestDialog = false) }
        startBreakTimer()
    }

    /**
     * Đóng dialog "Thất bại"
     */
    fun onDismissFailedDialog() {
        _uiState.update { it.copy(showFailedDialog = false) }
    }

    /**
     * Mở dialog Cài đặt
     */
    fun onSettingsClick() {
        _uiState.update { it.copy(showSettingsDialog = true) }
    }

    /**
     * Đóng dialog Cài đặt
     */
    fun onDismissSettingsDialog() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    /**
     * Áp dụng cài đặt thời gian mới
     */
    fun onSettingsApplied(newFocus: Long, newBreak: Long) {
        val newFocusMillis = newFocus * 60 * 1000L
        val newBreakMillis = newBreak * 60 * 1000L

        _uiState.update {
            val newState = it.copy(
                focusDurationMillis = newFocusMillis,
                breakDurationMillis = newBreakMillis,
                showSettingsDialog = false
            )
            // Logic này cập nhật thời gian hiển thị nếu timer không chạy
            if (!newState.isTimerRunning) {
                // Nếu đang nghỉ, không cập nhật, nếu không thì cập nhật
                if (newState.pomodoroState == PomodoroState.Ready || newState.pomodoroState == PomodoroState.Failed) {
                    newState.copy(remainingTimeMillis = newFocusMillis)
                } else {
                    newState
                }
            } else {
                newState
            }
        }
    }

    // --- 3. Logic nội bộ của ViewModel ---

    /**
     * Bắt đầu timer TẬP TRUNG (Focus)
     */
    private fun startTimer() {
        timerJob?.cancel()

        _uiState.update {
            it.copy(
                pomodoroState = PomodoroState.Running,
                isTimerRunning = true,
                remainingTimeMillis = it.focusDurationMillis
            )
        }

        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingTimeMillis > 0) {
                delay(1000L)
                _uiState.update {
                    it.copy(remainingTimeMillis = it.remainingTimeMillis - 1000L)
                }
            }

            // Khi đếm về 0 -> Hoàn thành
            _uiState.update {
                it.copy(
                    pomodoroState = PomodoroState.Finished,
                    isTimerRunning = false,
                    remainingTimeMillis = 0
                )
            }
        }
    }

    private fun stopTimer(isFailed: Boolean) {
        timerJob?.cancel()
        timerJob = null

        if (isFailed) {
            _uiState.update {
                it.copy(
                    pomodoroState = PomodoroState.Failed,
                    isTimerRunning = false,
                    remainingTimeMillis = it.focusDurationMillis,
                    showFailedDialog = true
                )
            }
        }
    }

    /**
     * (THÊM MỚI) Bắt đầu timer NGHỈ NGƠI (Break)
     */
    private fun startBreakTimer() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                pomodoroState = PomodoroState.Break,
                isTimerRunning = true, // Timer đang chạy
                remainingTimeMillis = it.breakDurationMillis // Đặt thời gian nghỉ
            )
        }

        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingTimeMillis > 0) {
                delay(1000L)
                _uiState.update {
                    it.copy(remainingTimeMillis = it.remainingTimeMillis - 1000L)
                }
            }

            // Hết giờ nghỉ -> Tự động về Ready
            _uiState.update {
                it.copy(
                    pomodoroState = PomodoroState.Ready,
                    isTimerRunning = false,
                    remainingTimeMillis = it.focusDurationMillis // Reset về thời gian focus
                )
            }
        }
    }

    /**
     * (THÊM MỚI) Bỏ qua (skip) thời gian nghỉ
     */
    private fun skipBreak() {
        timerJob?.cancel() // Dừng timer nghỉ
        _uiState.update {
            it.copy(
                pomodoroState = PomodoroState.Ready,
                isTimerRunning = false,
                remainingTimeMillis = it.focusDurationMillis // Reset về thời gian focus
            )
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}