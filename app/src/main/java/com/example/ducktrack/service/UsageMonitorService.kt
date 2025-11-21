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
import com.example.ducktrack.data.BaselineInfo
import com.example.ducktrack.data.LimitsStore
import com.example.ducktrack.utils.usageManager
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsageMonitorService : Service() {

    private val TAG = "UsageMonitorService"
    private val CHANNEL_ID = "usage_monitor_channel"
    private val NOTIFICATION_ID = 1001

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Lưu trạng thái: packageName -> (startTime, accumulatedMs)
    private val sessionMap = mutableMapOf<String, SessionData>()
    private data class SessionData(var startTime: Long, var accumulatedMs: Long = 0L)

    // Chống double-click "Thêm 15 phút"

    // (Các hàm sessionMap, lastExtendRequestTime, onCreate giữ nguyên)
    private val sessionMap = mutableMapOf<String, SessionData>()
    private data class SessionData(var startTime: Long, var accumulatedMs: Long = 0L)

    private val lastExtendRequestTime = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel() // Gọi hàm đã sửa
        startForeground(NOTIFICATION_ID, buildNotification())
        lastTickTime = System.currentTimeMillis()
    }

    // (Hàm onStartCommand, startMonitoring, checkUsageLimits, getCurrentForegroundApp, getTodayUsage, showBlockOverlay, handleExtendTime, handleRemoveLimit giữ nguyên)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "Service onStartCommand action=$action")

        when (action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopSelf()
            ACTION_EXTEND_TIME -> intent?.let { handleExtendTime(it) }
            ACTION_REMOVE_LIMIT -> intent?.let { handleRemoveLimit(it) } // block screen "Xóa giới hạn hôm nay"
        }

        return START_STICKY
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            Log.d(TAG, "Start monitoring...")

            while (isActive) {
                val today = todayString()
                if (today != currentDay) {
                    Log.d(TAG, "New day detected -> reset all usage & skip-today flags")
                    currentDay = today
                    usedMsMap.clear()
                    lastKnownBaselines.clear()
                    // Clear skip-today flags (SharedPreferences sẽ auto reset khi load)
                    clearSkipSetIfNewDay()
                }

                checkUsageLimits()
                delay(2000) // check mỗi 2 giây
            }
        }
    }

    /**
     * Logic:
     * - Mỗi vòng lặp:
     *   1. Cộng thời gian cho app đã ở foreground trong khoảng (lastTickTime -> now).
     *   2. Cập nhật app foreground hiện tại từ UsageEvents.
     *   3. Đọc limits + baseline, reset usage nếu user vừa set/đổi limit.
     *   4. Nếu app đó được đánh dấu "skip hôm nay" từ màn block → bỏ qua limit.
     *   5. Nếu không, kiểm tra có vượt limit hay chưa.
     */
    private suspend fun checkUsageLimits() {
        try {
            val now = System.currentTimeMillis()

            // 1) Cập nhật usage cho app foreground của vòng trước
            val prevPackage = lastForegroundPackage
            val delta = now - lastTickTime
            if (prevPackage != null && delta in 0L..60_000L) {
                val old = usedMsMap[prevPackage] ?: 0L
                usedMsMap[prevPackage] = old + delta
            }
            lastTickTime = now

            // 2) Tìm app foreground hiện tại (nếu không lấy được thì vẫn giữ app cũ)
            val fg = getCurrentForegroundAppRaw()
            if (fg != null) {
                lastForegroundPackage = fg
            }
            val currentApp = lastForegroundPackage ?: return

            // 3) Đọc limits
            val limitsStore = LimitsStore(applicationContext)
            val limits = limitsStore.getAll() // Map<packageName, minutes>
            if (limits.isEmpty()) return

            // Xoá usage của những app không còn limit (cho sạch map)
            usedMsMap.keys.retainAll(limits.keys)

            // Chỉ xử lý nếu app này có giới hạn

            val limitMinutes = limits[currentApp] ?: return
            val limitMs = limitMinutes * 60_000L
            val totalUsed = usedMsMap[currentApp] ?: 0L

            // Cập nhật session (hiện tại chủ yếu dùng getTodayUsage)
            val session = sessionMap.getOrPut(currentApp) {
                SessionData(System.currentTimeMillis())

            if (skipTodaySet.contains(currentApp)) {
                Log.i(
                    TAG,
                    "⏭ Bỏ qua giới hạn cho $currentApp trong ngày $currentDay (đã bấm 'Xóa giới hạn' trên màn block)"
                )
                // Không block trong hôm nay, nhưng ngày mai limit hoạt động lại
                return

            }

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

    /**
     * Lấy app đang chạy foreground trong ~5 giây gần nhất
     */
    private fun getCurrentForegroundApp(): String? {

     * Lấy app foreground mới nhất trong 5s gần đây (dựa vào event MOVE_TO_FOREGROUND)
     */
    private fun getCurrentForegroundAppRaw(): String? {

        val usm: UsageStatsManager = usageManager(applicationContext)
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 5000 // 5 giây trước

        val events: UsageEvents = usm.queryEvents(startTime, endTime)
        var latestPackage: String? = null
        var latestTime = 0L

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                if (event.timeStamp > latestTime) {
                    latestTime = event.timeStamp
                    latestPackage = event.packageName
                }
            }
        }

        return latestPackage
    }

    /**
     * Lấy tổng thời gian sử dụng app trong ngày hôm nay (từ 0h)
     */

    private fun getTodayUsage(packageName: String): Long {
        val usm: UsageStatsManager = usageManager(applicationContext)
        val end = System.currentTimeMillis()
        val start = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
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
            // DÙNG startService để tránh ForegroundServiceDidNotStartInTimeException
            startService(intent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

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
            limitsStore.setLimitWithBaseline(packageName, newLimit, baselineMs = 0L)
            Log.d(TAG, "Extended $packageName by 15 minutes: $currentLimit -> $newLimit")
        }
    }

    /**
     * Xử lý "Xóa giới hạn"
     */

    private fun handleRemoveLimit(intent: Intent) {
        val packageName = intent.getStringExtra("packageName") ?: return
        Log.d(TAG, "Service onStartCommand action=REMOVE_LIMIT for $packageName (BLOCK SCREEN)")

        scope.launch {
            // 1. Đánh dấu app này skip limit trong hôm nay
            val set = loadSkipSetForToday()
            if (!set.contains(packageName)) {
                set.add(packageName)
                saveSkipSetToday(set)
            }

            // 2. Có thể reset usage trong session hiện tại cho nhẹ nhàng
            usedMsMap.remove(packageName)
            lastKnownBaselines.remove(packageName)

            Log.d(
                TAG,
                "Marked $packageName as 'skip limit for today' (will reset next day). Limit vẫn còn trong LimitsStore."
            )
        }
    }

    /**
     * SỬA LỖI: Thêm kiểm tra API Level 26
     */
    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager == null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Usage Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Theo dõi thời gian sử dụng ứng dụng"
        }

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

}

