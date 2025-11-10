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

// 1. ViewModel -> AndroidViewModel(application)
class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    // 2.  Lấy repository từ Application
    private val repository = (application as MyApplication).repository

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        // 3. Cách collect Flow từ repository
        repository.unlockedSeeds
            .onEach { unlockedSet ->
                _uiState.update {
                    it.copy(availableSeeds = unlockedSet.toList())
                }
            }
            .launchIn(viewModelScope) // Tự động chạy và hủy
    }
    fun onMainButtonClick() {
        val currentState = _uiState.value
        when (currentState.pomodoroState) {
            PomodoroState.Running -> stopTimer(isFailed = true)
            PomodoroState.Ready, PomodoroState.Finished, PomodoroState.Failed -> startTimer()
            PomodoroState.Break -> skipBreak()
        }
    }
    fun onSeedSelected(seed: SeedType) {
        _uiState.update { it.copy(selectedSeed = seed) }
    }
    fun onHarvestClick() {
        _uiState.update { it.copy(showHarvestDialog = true) }
    }


    // 4. onDismissHarvestDialog phải dùng viewModelScope
    fun onDismissHarvestDialog() {
        viewModelScope.launch { // <-- Bọc trong launch vì repository là suspend
            val pointsToAdd = 50
            val seedToPlant = _uiState.value.selectedSeed

            repository.addPoints(pointsToAdd)
            repository.addGrownTree(seedToPlant)

            startBreakTimer()
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
            if (!newState.isTimerRunning) {
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
    private fun startBreakTimer() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                pomodoroState = PomodoroState.Break,
                isTimerRunning = true,
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