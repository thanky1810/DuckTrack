package com.example.ducktrack.ui.main.promodoro

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.MyApplication
import com.example.ducktrack.R // Import R để lấy ID nhạc
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

class PromodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MyApplication).repository

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    // --- AUDIO PLAYERS ---
    private var backgroundPlayer: MediaPlayer? = null
    private var effectPlayer: MediaPlayer? = null

    init {
        repository.unlockedSeeds
            .onEach { unlockedSet ->
                _uiState.update { it.copy(availableSeeds = unlockedSet.toList()) }
            }
            .launchIn(viewModelScope)
    }

    // --- HÀM CHỌN NHẠC TỪ UI ---
    fun onSoundSelected(sound: BackgroundSound) {
        _uiState.update { it.copy(selectedSound = sound) }

        // Nếu đang chạy timer thì đổi nhạc luôn
        if (_uiState.value.isTimerRunning) {
            playBackgroundMusic()
        }
    }

    // --- LOGIC PHÁT NHẠC NỀN ---
    private fun playBackgroundMusic() {
        // 1. Giải phóng player cũ nếu có
        backgroundPlayer?.release()
        backgroundPlayer = null

        val sound = _uiState.value.selectedSound
        if (sound.resId != null) {
            // 2. Tạo player mới
            backgroundPlayer = MediaPlayer.create(getApplication(), sound.resId)
            backgroundPlayer?.isLooping = true // Lặp lại vô tận
            backgroundPlayer?.start()
        }
    }

    private fun pauseBackgroundMusic() {
        if (backgroundPlayer?.isPlaying == true) {
            backgroundPlayer?.pause()
        }
    }

    private fun resumeBackgroundMusic() {
        if (_uiState.value.selectedSound != BackgroundSound.OFF && backgroundPlayer != null) {
            backgroundPlayer?.start()
        } else {
            // Trường hợp player bị null do thu hồi bộ nhớ hoặc chưa init
            playBackgroundMusic()
        }
    }

    private fun stopBackgroundMusic() {
        backgroundPlayer?.stop()
        backgroundPlayer?.release()
        backgroundPlayer = null
    }

    // --- LOGIC PHÁT HIỆU ỨNG (Chuông / Ending) ---
    private fun playEffect(resId: Int) {
        // Tạm nhỏ nhạc nền nếu đang phát (tuỳ chọn, ở đây mình giữ nguyên)
        effectPlayer?.release()
        effectPlayer = MediaPlayer.create(getApplication(), resId)
        effectPlayer?.start()
        effectPlayer?.setOnCompletionListener {
            it.release()
            effectPlayer = null
        }
    }

    // --- CẬP NHẬT CÁC HÀM CŨ ---

    fun onMainButtonClick() {
        val currentState = _uiState.value
        if (currentState.isTimerRunning) {
            // Đang chạy -> Bấm dừng
            if (currentState.promodoroState == PromodoroState.Running) {
                stopTimer(isFailed = true)
            } else {
                pauseTimer()
            }
        } else {
            // Đang dừng -> Bấm chạy
            resumeTimer()
        }
    }

    private fun resumeTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isTimerRunning = true) }

        // ==> BẮT ĐẦU PHÁT NHẠC NỀN
        playBackgroundMusic()

        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingTimeMillis > 0) {
                delay(1000L)
                _uiState.update {
                    it.copy(remainingTimeMillis = it.remainingTimeMillis - 1000L)
                }
            }
            handleTimerFinished()
        }
    }

    private fun handleTimerFinished() {
        // Dừng nhạc nền khi hết giờ
        stopBackgroundMusic()

        val state = _uiState.value

        if (state.promodoroState == PromodoroState.Running) {
            // KẾT THÚC PHIÊN TẬP TRUNG
            autoHarvest()
            val completedSessions = state.currentSessionCount + 1

            if (completedSessions >= state.sessionsBeforeLongBreak) {
                // ==> HOÀN THÀNH TOÀN BỘ: Phát nhạc Ending
                playEffect(R.raw.ending_effect)

                _uiState.update {
                    it.copy(
                        promodoroState = PromodoroState.Finished,
                        isTimerRunning = false,
                        currentSessionCount = completedSessions,
                        remainingTimeMillis = 0
                    )
                }
            } else {
                // ==> CHUYỂN QUA NGHỈ: Phát tiếng chuông
                playEffect(R.raw.japanese_school_bell)

                val isLongBreak = (completedSessions % 4 == 0)
                val breakTime = if (isLongBreak) state.longBreakDurationMillis else state.breakDurationMillis

                _uiState.update {
                    it.copy(
                        promodoroState = PromodoroState.Break,
                        currentSessionCount = completedSessions,
                        remainingTimeMillis = breakTime,
                        isTimerRunning = true
                    )
                }
                resumeTimer() // Tự động chạy nghỉ (và nhạc nền lại phát nếu user chọn nhạc)
            }

        } else if (state.promodoroState == PromodoroState.Break) {
            // ==> HẾT GIỜ NGHỈ: Phát tiếng chuông để báo vào làm việc
            playEffect(R.raw.japanese_school_bell)

            _uiState.update {
                it.copy(
                    promodoroState = PromodoroState.Running,
                    remainingTimeMillis = it.focusDurationMillis,
                    isTimerRunning = true
                )
            }
            resumeTimer()
        }
    }

    fun stopTimer(isFailed: Boolean) {
        timerJob?.cancel()
        // ==> DỪNG NHẠC NỀN
        stopBackgroundMusic()

        if (isFailed) {
            _uiState.update {
                it.copy(
                    promodoroState = PromodoroState.Failed,
                    isTimerRunning = false,
                    remainingTimeMillis = it.focusDurationMillis,
                    showFailedDialog = true
                )
            }
        } else {
            pauseTimer()
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        // ==> TẠM DỪNG NHẠC NỀN
        pauseBackgroundMusic()
        _uiState.update { it.copy(isTimerRunning = false) }
    }

    fun startNewSession() {
        val focusTime = _uiState.value.focusDurationMillis
        _uiState.update {
            it.copy(
                promodoroState = PromodoroState.Running,
                currentSessionCount = 0,
                remainingTimeMillis = focusTime,
                isTimerRunning = true
            )
        }
        resumeTimer()
    }

    // QUAN TRỌNG: Giải phóng tài nguyên khi thoát màn hình
    override fun onCleared() {
        super.onCleared()
        backgroundPlayer?.release()
        effectPlayer?.release()
        backgroundPlayer = null
        effectPlayer = null
    }

    // Các hàm khác giữ nguyên...
    fun onSeedSelected(seed: SeedType) { _uiState.update { it.copy(selectedSeed = seed) } }
    fun autoHarvest() {
        viewModelScope.launch {
            // Cộng điểm
            repository.addPoints(50)

            // Tạo chuỗi cấu hình: Focus/Break/Sessions/LongBreak
            // Ví dụ: 25/5/4/15
            val f = _uiState.value.focusDurationMillis / 60000
            val b = _uiState.value.breakDurationMillis / 60000
            val s = _uiState.value.sessionsBeforeLongBreak
            val l = _uiState.value.longBreakDurationMillis / 60000

            val configString = "$f / $b / $s / $l"

            // Lưu cây kèm cấu hình lên Cloud
            repository.addGrownTreeToCloud(_uiState.value.selectedSeed, configString)
        }
    }


    fun onHarvestClick() {}
    fun onDismissHarvestDialog() { _uiState.update { it.copy(promodoroState = PromodoroState.Ready, currentSessionCount = 0, isTimerRunning = false) } }
    fun onSettingsClick() { _uiState.update { it.copy(showSettingsDialog = true) } }
    fun onDismissSettingsDialog() { _uiState.update { it.copy(showSettingsDialog = false) } }
    fun onDismissFailedDialog() { _uiState.update { it.copy(showFailedDialog = false) } }
    fun onSettingsApplied(f: Long, b: Long, l: Long, s: Int) {
        val newFocusMillis = f * 60 * 1000L
        val newBreakMillis = b * 60 * 1000L
        val newLongBreakMillis = l * 60 * 1000L
        _uiState.update {
            it.copy(
                focusDurationMillis = newFocusMillis,
                breakDurationMillis = newBreakMillis,
                longBreakDurationMillis = newLongBreakMillis,
                sessionsBeforeLongBreak = s,
                remainingTimeMillis = newFocusMillis,
                showSettingsDialog = false
            )
        }
    }
}