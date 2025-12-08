package com.example.ducktrack.ui.main.pomodoro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.MainActivity
import com.example.ducktrack.MyApplication
import com.example.ducktrack.R
import com.example.ducktrack.data.UserPreferences
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
    private val userPrefs = UserPreferences(application)

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var backgroundPlayer: MediaPlayer? = null
    private var effectPlayer: MediaPlayer? = null

    // Biến lưu số bộ phiên hiện tại trong ngày (Mặc định là 1)
    private var currentDailySetIndex: Int = 1
    // Biến lưu ngày hiện tại để reset
    private var lastActiveDay: Int = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)

    init {
        repository.unlockedSeeds.onEach { unlockedSet ->
            _uiState.update { it.copy(availableSeeds = unlockedSet.toList()) }
        }.launchIn(viewModelScope)

        userPrefs.isVibrationEnabled.onEach { enabled ->
            _uiState.update { it.copy(isVibrationEnabled = enabled) }
        }.launchIn(viewModelScope)

        userPrefs.isKeepScreenOn.onEach { enabled ->
            _uiState.update { it.copy(isKeepScreenOn = enabled) }
        }.launchIn(viewModelScope)
    }

    // --- 1. XỬ LÝ HỦY PHIÊN & TRỪ ĐIỂM ---
    fun cancelSession(isHomeExit: Boolean) {
        val state = _uiState.value

        if (state.pomodoroState == PomodoroState.Running || state.pomodoroState == PomodoroState.Break) {
            val remainingSessions = state.sessionsBeforeLongBreak - state.currentSessionCount

            if (remainingSessions > 0) {
                val penaltyPerSession = if (isHomeExit) 50 else 25
                val totalPenalty = remainingSessions * penaltyPerSession

                viewModelScope.launch {
                    repository.deductPoints(totalPenalty)
                }

                if (isHomeExit) {
                    sendFailureNotification(totalPenalty)
                }
            }
        }
        stopTimer(isFailed = true)
    }

    private fun sendFailureNotification(penalty: Int) {
        val context = getApplication<Application>()
        val channelId = "pomodoro_fail_channel"
        val notificationId = 999

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Cảnh báo tập trung"
            val descriptionText = "Thông báo khi bạn thoát ứng dụng trong lúc tập trung"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.duck_crying)
            .setContentTitle("Quy trình thất bại! \uD83D\uDE2D")
            .setContentText("Bạn đã thoát ứng dụng. Bị trừ $penalty điểm sao.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Bạn đã thoát ứng dụng. Bị trừ $penalty điểm sao."))

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // --- 2. HÀM RUNG ---
    private fun vibratePhone() {
        if (!_uiState.value.isVibrationEnabled) return
        val context = getApplication<Application>()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    // --- 3. XỬ LÝ ÂM THANH ---
    fun onSoundSelected(sound: BackgroundSound) {
        _uiState.update { it.copy(selectedSound = sound) }
        if (_uiState.value.isTimerRunning) playBackgroundMusic()
    }
    private fun playBackgroundMusic() {
        backgroundPlayer?.release(); backgroundPlayer = null
        val sound = _uiState.value.selectedSound
        if (sound.resId != null) {
            backgroundPlayer = MediaPlayer.create(getApplication(), sound.resId)
            backgroundPlayer?.isLooping = true
            backgroundPlayer?.start()
        }
    }
    private fun pauseBackgroundMusic() { if (backgroundPlayer?.isPlaying == true) backgroundPlayer?.pause() }
    private fun stopBackgroundMusic() { backgroundPlayer?.stop(); backgroundPlayer?.release(); backgroundPlayer = null }
    private fun playEffect(resId: Int) {
        effectPlayer?.release()
        effectPlayer = MediaPlayer.create(getApplication(), resId)
        effectPlayer?.start()
        effectPlayer?.setOnCompletionListener { it.release(); effectPlayer = null }
    }

    // --- 4. TIMER CONTROL ---
    fun onMainButtonClick() {
        val currentState = _uiState.value
        if (currentState.isTimerRunning) {
            if (currentState.pomodoroState == PomodoroState.Running) {
                pauseTimer()
            } else {
                pauseTimer()
            }
        } else {
            resumeTimer()
        }
    }

    private fun resumeTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isTimerRunning = true) }
        playBackgroundMusic()

        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingTimeMillis > 0) {
                delay(1000L)
                _uiState.update { it.copy(remainingTimeMillis = it.remainingTimeMillis - 1000L) }
            }
            handleTimerFinished()
        }
    }

    private fun handleTimerFinished() {
        stopBackgroundMusic()
        val state = _uiState.value
        vibratePhone()

        if (state.pomodoroState == PomodoroState.Running) {
            // --- SỬA LOGIC TẠI ĐÂY ---
            // 1. Lưu cây TRƯỚC KHI tăng đếm phiên
            autoHarvest(
                currentSession = state.currentSessionCount,
                currentSet = currentDailySetIndex
            )

            // 2. Tăng số phiên đã hoàn thành
            val completedSessions = state.currentSessionCount + 1

            if (completedSessions >= state.sessionsBeforeLongBreak) {
                // Đã xong 1 bộ (ví dụ 4/4) -> Nghỉ dài & Kết thúc
                // Tăng bộ đếm set cho lần sau (ĐỂ DÀNH CHO LẦN CHẠY TIẾP THEO)
                currentDailySetIndex++

                playEffect(R.raw.ending_effect)
                _uiState.update { it.copy(pomodoroState = PomodoroState.Finished, isTimerRunning = false, currentSessionCount = completedSessions, remainingTimeMillis = 0) }
            } else {
                // Chưa xong bộ -> Nghỉ ngắn
                playEffect(R.raw.japanese_school_bell)
                val breakTime = state.breakDurationMillis

                _uiState.update { it.copy(pomodoroState = PomodoroState.Break, currentSessionCount = completedSessions, remainingTimeMillis = breakTime, isTimerRunning = true) }
                resumeTimer()
            }
        } else if (state.pomodoroState == PomodoroState.Break) {
            // Hết giờ nghỉ -> Quay lại tập trung
            playEffect(R.raw.japanese_school_bell)
            _uiState.update { it.copy(pomodoroState = PomodoroState.Running, remainingTimeMillis = it.focusDurationMillis, isTimerRunning = true) }
            resumeTimer()
        }
    }

    fun stopTimer(isFailed: Boolean) {
        timerJob?.cancel()
        stopBackgroundMusic()
        if (isFailed) {
            _uiState.update { it.copy(pomodoroState = PomodoroState.Failed, isTimerRunning = false, remainingTimeMillis = it.focusDurationMillis, showFailedDialog = true) }
        } else {
            pauseTimer()
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        pauseBackgroundMusic()
        _uiState.update { it.copy(isTimerRunning = false) }
    }

    fun startNewSession() {
        // Khi bấm Bắt đầu (từ trạng thái Ready/Finished), reset phiên về 0
        // Nhưng giữ nguyên currentDailySetIndex (đã được tăng ở lần finish trước hoặc giữ nguyên)
        val focusTime = _uiState.value.focusDurationMillis
        _uiState.update { it.copy(pomodoroState = PomodoroState.Running, currentSessionCount = 0, remainingTimeMillis = focusTime, isTimerRunning = true) }
        resumeTimer()
    }

    override fun onCleared() {
        super.onCleared()
        backgroundPlayer?.release()
        effectPlayer?.release()
    }

    // --- 5. LOGIC TRỒNG CÂY (ĐÃ SỬA) ---
    fun onSeedSelected(seed: SeedType) { _uiState.update { it.copy(selectedSeed = seed) } }

    // Nhận tham số session và set từ bên ngoài để đảm bảo tính đúng thời điểm
    fun autoHarvest(currentSession: Int, currentSet: Int) {
        viewModelScope.launch {
            repository.addPoints(50)

            val f = _uiState.value.focusDurationMillis / 60000
            val b = _uiState.value.breakDurationMillis / 60000
            val s = _uiState.value.sessionsBeforeLongBreak
            val l = _uiState.value.longBreakDurationMillis / 60000
            val configStr = "$f / $b / $s / $l"

            // Check ngày mới để reset bộ đếm Set
            val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
            if (today != lastActiveDay) {
                currentDailySetIndex = 1
                lastActiveDay = today
            }

            // Tính toán chỉ số phiên hiển thị (Bắt đầu từ 1)
            // currentSession lúc này là số phiên ĐÃ xong TRƯỚC ĐÓ.
            // Ví dụ: Bắt đầu phiên đầu tiên -> currentSession = 0.
            // Hoàn thành phiên này -> Đây là phiên thứ (0 + 1) = 1.
            val sessionIndexToSave = currentSession + 1

            // Gọi repository lưu cây với chỉ số ĐÚNG
            repository.addGrownTreeToCloud(
                _uiState.value.selectedSeed,
                configStr,
                currentSet, // Sử dụng bộ hiện tại
                sessionIndexToSave
            )
        }
    }

    fun onDismissHarvestDialog() {
        // Khi đóng dialog thu hoạch (đã xong 1 set) -> Reset về Ready
        _uiState.update { it.copy(pomodoroState = PomodoroState.Ready, currentSessionCount = 0, isTimerRunning = false) }
    }

    fun onSettingsClick() { _uiState.update { it.copy(showSettingsDialog = true) } }
    fun onDismissSettingsDialog() { _uiState.update { it.copy(showSettingsDialog = false) } }
    fun onDismissFailedDialog() { _uiState.update { it.copy(showFailedDialog = false) } }
    fun onSettingsApplied(f: Long, b: Long, l: Long, s: Int) {
        val newFocusMillis = f * 60 * 1000L; val newBreakMillis = b * 60 * 1000L; val newLongBreakMillis = l * 60 * 1000L
        _uiState.update { it.copy(focusDurationMillis = newFocusMillis, breakDurationMillis = newBreakMillis, longBreakDurationMillis = newLongBreakMillis, sessionsBeforeLongBreak = s, remainingTimeMillis = newFocusMillis, showSettingsDialog = false) }
    }
}