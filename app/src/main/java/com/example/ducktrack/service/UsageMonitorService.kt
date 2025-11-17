package com.example.ducktrack.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context // <-- Thêm import
import android.content.Intent
import android.os.Build // <-- Thêm import
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ducktrack.data.LimitsStore
import com.example.ducktrack.utils.usageManager
import kotlinx.coroutines.*
import java.util.Calendar

class UsageMonitorService : Service() {

    private val TAG = "UsageMonitorService"
    private val CHANNEL_ID = "usage_monitor_channel"
    private val NOTIFICATION_ID = 1001

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // (Các hàm sessionMap, lastExtendRequestTime, onCreate giữ nguyên)
    private val sessionMap = mutableMapOf<String, SessionData>()
    private data class SessionData(var startTime: Long, var accumulatedMs: Long = 0L)
    private val lastExtendRequestTime = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel() // Gọi hàm đã sửa
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    // (Hàm onStartCommand, startMonitoring, checkUsageLimits, getCurrentForegroundApp, getTodayUsage, showBlockOverlay, handleExtendTime, handleRemoveLimit giữ nguyên)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "Service onStartCommand action=$action")

        when (action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopSelf()
            ACTION_EXTEND_TIME -> intent?.let { handleExtendTime(it) }
            ACTION_REMOVE_LIMIT -> intent?.let { handleRemoveLimit(it) }
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

            val limitMinutes = limits[currentApp] ?: return
            val limitMs = limitMinutes * 60_000L

            val session = sessionMap.getOrPut(currentApp) {
                SessionData(System.currentTimeMillis())
            }

            val todayUsage = getTodayUsage(currentApp)
            val totalUsed = todayUsage + session.accumulatedMs

            Log.i(TAG, "🔔 ĐANG GIÁM SÁT: $currentApp")
            Log.i(
                TAG,
                "-> Đã dùng: ${totalUsed / 60000} phút / Giới hạn: $limitMinutes phút (Tổng ms: $totalUsed / $limitMs)"
            )

            if (totalUsed >= limitMs) {
                Log.w(TAG, "⚠️⚠️⚠️ VƯỢT GIỚI HẠN: $currentApp. Hiển thị màn hình chặn!")
                showBlockOverlay(currentApp, limitMinutes)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking limits", e)
        }
    }

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

    private fun showBlockOverlay(packageName: String, limitMinutes: Int) {
        if (!android.provider.Settings.canDrawOverlays(applicationContext)) {
            Log.e(TAG, "❌ No overlay permission - cannot show block screen")
            return
        }

        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_BLOCK
            putExtra("packageName", packageName)
            putExtra("limitMinutes", limitMinutes)
        }

        Log.d(TAG, "OverlayService startService() called")

        try {
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start OverlayService", e)
        }
    }

    private fun handleExtendTime(intent: Intent) {
        val packageName = intent.getStringExtra("packageName") ?: return

        val now = System.currentTimeMillis()
        val last = lastExtendRequestTime[packageName] ?: 0L
        if (now - last < 3000L) {
            Log.d(TAG, "Ignore duplicate EXTEND_TIME for $packageName (within 3s)")
            return
        }
        lastExtendRequestTime[packageName] = now

        Log.d(TAG, "Service onStartCommand action=EXTEND_TIME for $packageName")

        scope.launch {
            val limitsStore = LimitsStore(applicationContext)
            val all = limitsStore.getAll()
            val currentLimit = all[packageName] ?: return@launch
            val newLimit = currentLimit + 15
            limitsStore.setLimit(packageName, newLimit)
            Log.d(TAG, "Extended $packageName by 15 minutes: $currentLimit -> $newLimit")
        }
    }

    private fun handleRemoveLimit(intent: Intent) {
        val packageName = intent.getStringExtra("packageName") ?: return
        Log.d(TAG, "Service onStartCommand action=REMOVE_LIMIT for $packageName")

        scope.launch {
            val limitsStore = LimitsStore(applicationContext)
            limitsStore.removeLimit(packageName)
            Log.d(TAG, "Removed limit for $packageName")
        }
    }

    /**
     * SỬA LỖI: Thêm kiểm tra API Level 26
     */
    private fun createNotificationChannel() {
        // Chỉ tạo channel trên API 26 (Oreo) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager == null) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Usage Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Theo dõi thời gian sử dụng ứng dụng"
            }

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