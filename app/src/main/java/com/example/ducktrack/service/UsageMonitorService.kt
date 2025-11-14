package com.example.ducktrack.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ducktrack.data.LimitsStore
import com.example.ducktrack.utils.usageManager
import kotlinx.coroutines.*
import java.util.Calendar

/**
 * Foreground Service theo dõi app đang sử dụng
 * Khi phát hiện vượt giới hạn → start OverlayService để chặn
 */
class UsageMonitorService : Service() {

    private val TAG = "UsageMonitorService" // <--- Lọc theo TAG này trong Logcat
    private val CHANNEL_ID = "usage_monitor_channel"
    private val NOTIFICATION_ID = 1001

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Lưu trạng thái: packageName -> (startTime, accumulatedMs)
    private val sessionMap = mutableMapOf<String, SessionData>()
    private data class SessionData(var startTime: Long, var accumulatedMs: Long = 0L)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")

        when (intent?.action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopSelf()
            ACTION_EXTEND_TIME -> handleExtendTime(intent)
            ACTION_REMOVE_LIMIT -> handleRemoveLimit(intent)
        }

        return START_STICKY
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            Log.d(TAG, "Start monitoring...")

            while (isActive) {
                checkUsageLimits()
                delay(2000) // Check mỗi 2 giây
            }
        }
    }

    private suspend fun checkUsageLimits() {
        try {
            val limitsStore = LimitsStore(applicationContext)
            val limits = limitsStore.getAll() // Map<packageName, minutes>

            if (limits.isEmpty()) return

            val currentApp = getCurrentForegroundApp() ?: return

            // Dòng này sẽ lọc, chỉ app nào CÓ GIỚI HẠN (như TikTok) mới đi tiếp
            val limitMinutes = limits[currentApp] ?: return
            val limitMs = limitMinutes * 60_000L

            // Cập nhật session
            val session = sessionMap.getOrPut(currentApp) {
                SessionData(System.currentTimeMillis())
            }

            // Tính thời gian sử dụng
            val todayUsage = getTodayUsage(currentApp)
            val totalUsed = todayUsage + session.accumulatedMs

            // =================================================================
            // === LOG BẠN CẦN ĐỂ TEST ===
            // Log này sẽ xuất hiện mỗi 2 giây khi bạn mở app (ví dụ TikTok)
            // =================================================================
            Log.i(TAG, "🔔 ĐANG GIÁM SÁT: $currentApp")
            Log.i(TAG, "-> Đã dùng: ${totalUsed / 60000} phút / Giới hạn: $limitMinutes phút (Tổng ms: $totalUsed / $limitMs)")
            // =================================================================


            // Nếu vượt giới hạn → show overlay
            if (totalUsed >= limitMs) {

                // =================================================================
                // === LOG KHI VƯỢT GIỚI HẠN ===
                // Log này sẽ xuất hiện 1 lần ngay trước khi màn hình chặn hiện lên
                // =================================================================
                Log.w(TAG, "⚠️⚠️⚠️ VƯỢT GIỚI HẠN: $currentApp. Hiển thị màn hình chặn!")
                // =================================================================

                showBlockOverlay(currentApp, limitMinutes)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking limits", e)
        }
    }

    /**
     * Lấy app đang chạy foreground
     */
    private fun getCurrentForegroundApp(): String? {
        val usm: UsageStatsManager = usageManager(applicationContext)
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 5000 // 5 giây trước

        val events = usm.queryEvents(startTime, endTime)
        var currentApp: String? = null

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                currentApp = event.packageName
            }
        }

        return currentApp
    }

    /**
     * Lấy tổng thời gian sử dụng app trong ngày hôm nay
     */
    private fun getTodayUsage(packageName: String): Long {
        val usm: UsageStatsManager = usageManager(applicationContext)
        val end = System.currentTimeMillis()
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        return stats?.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
    }

    /**
     * Hiển thị màn hình chặn (OverlayService)
     */
    private fun showBlockOverlay(packageName: String, limitMinutes: Int) {
        // Kiểm tra quyền overlay trước
        if (!android.provider.Settings.canDrawOverlays(applicationContext)) {
            Log.e(TAG, "❌ No overlay permission - cannot show block screen")
            return
        }

        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_BLOCK
            putExtra("packageName", packageName)
            putExtra("limitMinutes", limitMinutes)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    /**
     * Xử lý "Xem tiếp 15 phút"
     */
    private fun handleExtendTime(intent: Intent) {
        val packageName = intent.getStringExtra("packageName") ?: return

        scope.launch {
            val limitsStore = LimitsStore(applicationContext)
            val currentLimit = limitsStore.getAll()[packageName] ?: return@launch

            // Tăng giới hạn thêm 15 phút
            limitsStore.setLimit(packageName, currentLimit + 15)
            Log.d(TAG, "Extended $packageName by 15 minutes")
        }
    }

    /**
     * Xử lý "Bỏ giới hạn"
     */
    private fun handleRemoveLimit(intent: Intent) {
        val packageName = intent.getStringExtra("packageName") ?: return

        scope.launch {
            val limitsStore = LimitsStore(applicationContext)
            limitsStore.removeLimit(packageName)
            Log.d(TAG, "Removed limit for $packageName")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Usage Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Theo dõi thời gian sử dụng ứng dụng"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DuckTrack đang chạy")
            .setContentText("Đang theo dõi thời gian sử dụng")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
        scope.cancel()
        Log.d(TAG, "Service onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_MONITORING = "START_MONITORING"
        const val ACTION_STOP_MONITORING = "STOP_MONITORING"
        const val ACTION_EXTEND_TIME = "EXTEND_TIME"
        const val ACTION_REMOVE_LIMIT = "REMOVE_LIMIT"
    }
}