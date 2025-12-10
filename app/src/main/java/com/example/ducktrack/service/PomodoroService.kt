package com.example.ducktrack.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.ducktrack.MainActivity
import com.example.ducktrack.R
import com.example.ducktrack.utils.formatTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PomodoroService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    private val _remainingTime = MutableStateFlow(0L)
    val remainingTime: StateFlow<Long> = _remainingTime.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    var onTimerFinished: (() -> Unit)? = null
    private var taskCount = 0

    inner class LocalBinder : Binder() {
        fun getService(): PomodoroService = this@PomodoroService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    fun startTimer(durationMillis: Long, totalTasks: Int) {
        taskCount = totalTasks
        _remainingTime.value = durationMillis
        _isRunning.value = true

        // Start Foreground ngay lập tức
        startForeground(NOTIFICATION_ID, buildNotification(durationMillis))

        timerJob?.cancel()
        timerJob = scope.launch {
            var timeLeft = durationMillis
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft -= 1000L
                _remainingTime.value = timeLeft

                // Cập nhật thông báo
                updateNotification(timeLeft)
            }
            _isRunning.value = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            onTimerFinished?.invoke()
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _isRunning.value = false
        // Giữ thông báo nhưng không cập nhật giây nữa
        updateNotification(_remainingTime.value, isPaused = true)
    }

    fun stopTimer() {
        timerJob?.cancel()
        _isRunning.value = false
        _remainingTime.value = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(NOTIFICATION_ID)
    }

    fun updateTaskCount(count: Int) {
        taskCount = count
        if (_isRunning.value) updateNotification(_remainingTime.value)
    }

    private fun updateNotification(timeMs: Long, isPaused: Boolean = false) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(timeMs, isPaused))
    }

    private fun buildNotification(timeMs: Long, isPaused: Boolean = false): android.app.Notification {
        val timeString = timeMs.formatTime()
        val title = if (isPaused) "Đang tạm dừng ($timeString)" else "Đang tập trung: $timeString"
        val content = "Còn $taskCount nhiệm vụ cần làm hôm nay."

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.duck_farming)
            .setContentTitle(title)
            .setContentText(content)
            // --- SỬA Ở ĐÂY: Tăng độ ưu tiên lên DEFAULT ---
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // --- QUAN TRỌNG: Chỉ rung/kêu lần đầu, các lần update sau im lặng ---
            .setOnlyAlertOnce(true)
            .setOngoing(true) // Không cho vuốt tắt
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Hiện trên màn hình khóa
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pomodoro Timer",
                // --- SỬA Ở ĐÂY: Tăng độ quan trọng lên DEFAULT ---
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Hiển thị đồng hồ đếm ngược"
                setSound(null, null) // Tắt tiếng chuông mặc định để không phiền
                enableVibration(false) // Tắt rung mặc định của channel
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        timerJob?.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 2002
        const val CHANNEL_ID = "pomodoro_timer_channel_v2" // Đổi tên ID để tạo channel mới settings mới
    }
}