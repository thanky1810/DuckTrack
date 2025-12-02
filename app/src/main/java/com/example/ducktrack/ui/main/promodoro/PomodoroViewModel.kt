package com.example.ducktrack.ui.main.promodoro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.MyApplication
import com.example.ducktrack.ui.main.garden.SeedType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MyApplication).repository

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        repository.unlockedSeeds
            .onEach { unlockedSet ->
                _uiState.update {
                    it.copy(availableSeeds = unlockedSet.toList())
                }
            }
            .launchIn(viewModelScope)
    }

    // --- SỬA LOGIC NÚT BẤM ---
    fun onMainButtonClick() {
        val currentState = _uiState.value
        when (currentState.pomodoroState) {
            PomodoroState.Running -> stopTimer(isFailed = true)
            PomodoroState.Ready, PomodoroState.Finished, PomodoroState.Failed -> startTimer()

            // Nếu đang ở trạng thái Break (Nghỉ)
            PomodoroState.Break -> {
                if (currentState.isTimerRunning) {
                    // Nếu đang chạy -> Bấm nút là Bỏ qua (Skip)
                    skipBreak()
                } else {
                    // Nếu đang dừng (mới thu hoạch xong) -> Bấm nút là Bắt đầu chạy giờ nghỉ
                    startBreakTimer()
                }
            }
        }
    }

    fun onSeedSelected(seed: SeedType) {
        _uiState.update { it.copy(selectedSeed = seed) }
    }

    fun onHarvestClick() {
        _uiState.update { it.copy(showHarvestDialog = true) }
    }

    // --- SỬA HÀM NÀY: KHÔNG TỰ ĐỘNG CHẠY NỮA ---
    fun onDismissHarvestDialog() {
        viewModelScope.launch {
            val pointsToAdd = 50
            val seedToPlant = _uiState.value.selectedSeed

            repository.addPoints(pointsToAdd)
            repository.addGrownTree(seedToPlant)

            // Thay vì startBreakTimer(), ta chỉ chuyển trạng thái sang chờ thôi
            waitForBreak()
        }
    }

    // --- HÀM MỚI: CHUYỂN SANG CHẾ ĐỘ CHỜ NGHỈ (CHƯA ĐẾM NGƯỢC) ---
    private fun waitForBreak() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                pomodoroState = PomodoroState.Break,
                isTimerRunning = false, // Đánh dấu là chưa chạy
                remainingTimeMillis = it.breakDurationMillis, // Reset về 5 phút (hoặc tùy chỉnh)
                showHarvestDialog = false
            )
        }
    }

    fun onDismissFailedDialog() { _uiState.update { it.copy(showFailedDialog = false) } }
    fun onSettingsClick() { _uiState.update { it.copy(showSettingsDialog = true) } }
    fun onDismissSettingsDialog() { _uiState.update { it.copy(showSettingsDialog = false) } }

    fun onSettingsApplied(newFocus: Long, newBreak: Long) {
        val newFocusMillis = newFocus * 60 * 1000L
        val newBreakMillis = newBreak * 60 * 1000L
        _uiState.update {
            val newState = it.copy(
                focusDurationMillis = newFocusMillis,
                breakDurationMillis = newBreakMillis,
                showSettingsDialog = false
            )
            // Nếu timer không chạy, cập nhật luôn số hiển thị
            if (!newState.isTimerRunning) {
                if (newState.pomodoroState == PomodoroState.Break) {
                    newState.copy(remainingTimeMillis = newBreakMillis)
                } else if (newState.pomodoroState == PomodoroState.Ready || newState.pomodoroState == PomodoroState.Failed) {
                    newState.copy(remainingTimeMillis = newFocusMillis)
                } else {
                    newState
                }
            } else {
                newState
            }
        }
    }

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
            _uiState.update {
                it.copy(
                    pomodoroState = PomodoroState.Finished,
                    isTimerRunning = false,
                    remainingTimeMillis = 0
                )
            }
        }
    }

    fun stopTimer(isFailed: Boolean) { // Public để FocusModeScreen gọi được
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
        } else {
            // Nếu dừng bình thường (reset)
            _uiState.update {
                it.copy(
                    pomodoroState = PomodoroState.Ready,
                    isTimerRunning = false,
                    remainingTimeMillis = it.focusDurationMillis
                )
            }
        }
    }

    private fun startBreakTimer() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                pomodoroState = PomodoroState.Break,
                isTimerRunning = true, // Bắt đầu chạy
                remainingTimeMillis = it.breakDurationMillis,
                showHarvestDialog = false
            )
        }
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingTimeMillis > 0) {
                delay(1000L)
                _uiState.update {
                    it.copy(remainingTimeMillis = it.remainingTimeMillis - 1000L)
                }
            }
            // Hết giờ nghỉ -> Quay về Ready
            _uiState.update {
                it.copy(
                    pomodoroState = PomodoroState.Ready,
                    isTimerRunning = false,
                    remainingTimeMillis = it.focusDurationMillis
                )
            }
        }
    }

    private fun skipBreak() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                pomodoroState = PomodoroState.Ready,
                isTimerRunning = false,
                remainingTimeMillis = it.focusDurationMillis
            )
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}