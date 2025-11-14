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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Foreground Service theo dõi app đang sử dụng
 * Khi phát hiện vượt giới hạn → start OverlayService để chặn
 */
class UsageMonitorService : Service() {

    private val TAG = "UsageMonitorService"
    private val CHANNEL_ID = "usage_monitor_channel"
    private val NOTIFICATION_ID = 1001

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Map lưu thời gian đã dùng TRONG NGÀY HIỆN TẠI: packageName -> usedMs
    private val usageMap = mutableMapOf<String, Long>()

    // Lưu app foreground gần nhất (do queryEvents không phải lúc nào cũng có event mới)
    private var lastForegroundApp: String? = null

    // Thời điểm lần check gần nhất (ms)
    private var lastCheckTime: Long = 0L

    // Key ngày hiện tại, dạng yyyy-MM-dd (để reset khi qua ngày mới)
    private var lastDayKey: String = currentDayKey()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand action=${intent?.action}")

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
        lastCheckTime = System.currentTimeMillis()
        lastDayKey = currentDayKey()
        usageMap.clear()

        monitorJob = scope.launch {
            Log.d(TAG, "Start monitoring...")

            while (isActive) {
                checkUsageLimits()
                delay(2000) // Check mỗi 2 giây
            }
        }
    }

    /**
     * Trả về key ngày hiện tại dạng yyyy-MM-dd
     */
    private fun currentDayKey(): String {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return df.format(Date())
    }

    private suspend fun checkUsageLimits() {
        try {
            val limitsStore = LimitsStore(applicationContext)
            val limits = limitsStore.getAll() // Map<packageName, minutes>
            if (limits.isEmpty()) return

            val now = System.currentTimeMillis()

            // Reset nếu qua ngày mới
            val dayKey = currentDayKey()
            if (dayKey != lastDayKey) {
                Log.d(TAG, "New day detected, reset usageMap")
                usageMap.clear()
                lastDayKey = dayKey
            }

            // Tính thời gian trôi qua từ lần check trước
            if (lastCheckTime == 0L) {
                lastCheckTime = now
                return
            }
            val dt = now - lastCheckTime
            if (dt <= 0L) {
                lastCheckTime = now
                return
            }
            lastCheckTime = now

            // App đang foreground hiện tại
            val currentApp = getCurrentForegroundApp()
            if (currentApp == null) {
                // Không có app tiền cảnh rõ ràng → không cộng thời gian cho ai
                return
            }

            // Chỉ quan tâm nếu app này có giới hạn
            val limitMinutes = limits[currentApp] ?: return
            val limitMs = limitMinutes * 60_000L

            // Cộng thêm dt vào thời gian đã dùng của app
            val prevUsed = usageMap[currentApp] ?: 0L
            val usedMs = prevUsed + dt
            usageMap[currentApp] = usedMs

            // LOG cho bạn test
            Log.i(TAG, "🔔 ĐANG GIÁM SÁT: $currentApp")
            Log.i(
                TAG,
                "-> Đã dùng: ${usedMs / 60000} phút / Giới hạn: $limitMinutes phút (Tổng ms: $usedMs / $limitMs)"
            )

            // Nếu vượt giới hạn → show overlay
            if (usedMs >= limitMs) {
                Log.w(TAG, "⚠️⚠️⚠️ VƯỢT GIỚI HẠN: $currentApp. Hiển thị màn hình chặn!")
                showBlockOverlay(currentApp, limitMinutes)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking limits", e)
        }
    }

    /**
     * Lấy app đang chạy foreground ổn định:
     * - Nếu 5s gần nhất có MOVE_TO_FOREGROUND → cập nhật app đó
     * - Nếu không có event mới → dùng lại lastForegroundApp
     * - Nếu có MOVE_TO_BACKGROUND đúng app hiện tại → clear
     */
    private fun getCurrentForegroundApp(): String? {
        val usm: UsageStatsManager = usageManager(applicationContext)
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 5000 // 5 giây trước

        val events = usm.queryEvents(startTime, endTime)
        var result = lastForegroundApp

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    result = event.packageName
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (result == event.packageName) {
                        result = null
                    }
                }
            }
        }

        lastForegroundApp = result
        return result
    }

    /**
     * Hiển thị màn hình chặn (OverlayService)
     */
    private fun showBlockOverlay(packageName: String, limitMinutes: Int) {
        Log.d(TAG, "showBlockOverlay() for $packageName, limit=$limitMinutes")

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

        try {
            startService(intent)
            Log.d(TAG, "OverlayService startService() called")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start OverlayService", e)
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

            // Reset lại bộ đếm trong ngày cho app đó (đếm lại từ 0 sau khi gia hạn)
            usageMap[packageName] = 0L
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
            usageMap.remove(packageName)
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
