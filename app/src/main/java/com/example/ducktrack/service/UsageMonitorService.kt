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

    // Map: mỗi app -> tổng ms đã dùng (tự đếm bằng đồng hồ)
    private val usedMsMap = mutableMapOf<String, Long>()

    // Lưu baseline cuối cùng để biết khi nào user SET / ĐỔI limit
    private val lastKnownBaselines = mutableMapOf<String, BaselineInfo>()

    // App foreground ở vòng lặp trước + thời điểm tick trước
    private var lastForegroundPackage: String? = null
    private var lastTickTime: Long = System.currentTimeMillis()

    // App hiện đang bị chặn (đang hiển thị overlay)
    private var blockedPackage: String? = null

    // Dùng để reset usage khi sang ngày mới
    private var currentDay: String = todayString()

    // Chống spam nút "Extend time"
    private val lastExtendRequestTime = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        lastTickTime = System.currentTimeMillis()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "Service onStartCommand action=$action")

        when (action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopSelf()
            ACTION_EXTEND_TIME -> intent?.let { handleExtendTime(it) }
            ACTION_REMOVE_LIMIT -> intent?.let { handleRemoveLimit(it) } // từ màn block
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
                    blockedPackage = null
                    clearSkipSetIfNewDay()
                    hideBlockOverlay() // đảm bảo tắt overlay nếu còn
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
     *   4. Nếu app đó được "skip hôm nay" (bấm Xóa giới hạn trên màn block) → bỏ qua limit.
     *   5. Nếu currentApp khác app đang bị chặn → ẩn overlay.
     *   6. Nếu currentApp vượt limit → hiển thị overlay cho app đó.
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
            if (limits.isEmpty()) {
                // Không còn app nào có limit -> ẩn overlay nếu đang hiện
                if (blockedPackage != null) {
                    hideBlockOverlay()
                    blockedPackage = null
                }
                return
            }

            // Xoá usage của những app không còn limit (cho sạch map)
            usedMsMap.keys.retainAll(limits.keys)

            val baselines = limitsStore.getAllBaselines()

            // Reset usage khi baseline (thời điểm set limit) thay đổi
            for ((pkg, baseline) in baselines) {
                val prev = lastKnownBaselines[pkg]
                if (prev == null ||
                    prev.day != baseline.day ||
                    prev.baselineMs != baseline.baselineMs
                ) {
                    Log.d(
                        TAG,
                        "Baseline changed for $pkg -> reset usage (day=${baseline.day}, baselineMs=${baseline.baselineMs})"
                    )
                    usedMsMap[pkg] = 0L
                    lastKnownBaselines[pkg] = baseline
                }
            }
            // Xoá baseline cũ của những app không còn baseline
            lastKnownBaselines.keys.retainAll(baselines.keys)

            // 4) Lấy danh sách app được "bỏ qua giới hạn hôm nay" (từ màn block)
            val skipTodaySet = loadSkipSetForToday()

            // 5) Nếu đang chặn app A, nhưng giờ foreground không phải A (hoặc A không còn limit / bị skip hôm nay) → tắt overlay
            blockedPackage?.let { blocked ->
                val hasLimit = limits.containsKey(blocked)
                val skippedToday = skipTodaySet.contains(blocked)
                if (currentApp != blocked || !hasLimit || skippedToday) {
                    Log.d(
                        TAG,
                        "Foreground changed or limit removed/skipToday -> hide overlay for $blocked"
                    )
                    hideBlockOverlay()
                    blockedPackage = null
                }
            }

            // 6) Kiểm tra limit cho app hiện tại
            val limitMinutes = limits[currentApp] ?: return
            val limitMs = limitMinutes * 60_000L
            val totalUsed = usedMsMap[currentApp] ?: 0L

            if (skipTodaySet.contains(currentApp)) {
                Log.i(
                    TAG,
                    "⏭ Bỏ qua giới hạn cho $currentApp trong ngày $currentDay (đã bấm 'Xóa giới hạn' trên màn block)"
                )
                return
            }

            Log.i(TAG, "🔔 ĐANG GIÁM SÁT: $currentApp")
            Log.i(
                TAG,
                "-> Đã dùng: ${totalUsed / 60000} phút / Giới hạn: $limitMinutes phút (Tổng ms: $totalUsed / $limitMs)"
            )

            if (totalUsed >= limitMs) {
                // Chỉ hiển thị overlay nếu chưa chặn app này
                if (blockedPackage != currentApp) {
                    Log.w(TAG, "⚠️⚠️⚠️ VƯỢT GIỚI HẠN: $currentApp. Hiển thị màn hình chặn!")
                    showBlockOverlay(currentApp, limitMinutes)
                    blockedPackage = currentApp
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking limits", e)
        }
    }

    /**
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
     * Hàm cũ dùng UsageStats để lấy tổng foreground từ 0h đến giờ.
     * Không dùng trong logic chính, nhưng giữ lại nếu cần về sau.
     */
    @Suppress("unused")
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

        Log.d(TAG, "OverlayService startService() called to SHOW_BLOCK for $packageName")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start OverlayService", e)
        }
    }

    private fun hideBlockOverlay() {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_HIDE_BLOCK
        }
        Log.d(TAG, "OverlayService startService() called to HIDE_BLOCK")
        try {
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide overlay", e)
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
            // Reset baseline để phiên mới tính từ lúc extend
            limitsStore.setLimitWithBaseline(packageName, newLimit, baselineMs = 0L)
            Log.d(TAG, "Extended $packageName by 15 minutes: $currentLimit -> $newLimit")
        }
    }

    /**
     * Nút "Xóa giới hạn" trên MÀN BLOCK:
     *  - KHÔNG xóa limit vĩnh viễn.
     *  - Chỉ đánh dấu: hôm nay bỏ qua giới hạn cho app này.
     *  - Sang ngày mới, limit hoạt động lại như cũ.
     *
     * Xóa VĨNH VIỄN vẫn là nút trong CARD (MainScreen -> vm.removeLimit -> limitsStore.removeLimit).
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

            // 2. Reset usage & trạng thái block cho app này
            usedMsMap.remove(packageName)
            lastKnownBaselines.remove(packageName)
            if (blockedPackage == packageName) {
                hideBlockOverlay()
                blockedPackage = null
            }

            Log.d(
                TAG,
                "Marked $packageName as 'skip limit for today'. Limit vẫn còn trong LimitsStore, sẽ hoạt động lại ngày mai."
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

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
        blockedPackage = null
        hideBlockOverlay()
        Log.d(TAG, "Service onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_MONITORING = "START_MONITORING"
        const val ACTION_STOP_MONITORING = "STOP_MONITORING"
        const val ACTION_EXTEND_TIME = "EXTEND_TIME"
        const val ACTION_REMOVE_LIMIT = "REMOVE_LIMIT"
    }

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // ===================== HỖ TRỢ "XÓA GIỚI HẠN TRONG NGÀY" =====================

    private fun skipPrefs() =
        getSharedPreferences("ducktrack_skip_limits", MODE_PRIVATE)

    /**
     * Load danh sách app được bỏ qua giới hạn trong NGÀY HIỆN TẠI.
     * Nếu stored day khác today -> reset.
     */
    private fun loadSkipSetForToday(): MutableSet<String> {
        val prefs = skipPrefs()
        val today = todayString()
        val savedDay = prefs.getString("day", null)
        if (savedDay != today) {
            // Reset cho ngày mới
            prefs.edit()
                .putString("day", today)
                .putStringSet("apps", emptySet())
                .apply()
            return mutableSetOf()
        }
        val set = prefs.getStringSet("apps", emptySet()) ?: emptySet()
        return set.toMutableSet()
    }

    /**
     * Lưu danh sách app được bỏ qua giới hạn trong ngày hiện tại.
     */
    private fun saveSkipSetToday(set: Set<String>) {
        val prefs = skipPrefs()
        prefs.edit()
            .putString("day", todayString())
            .putStringSet("apps", set.toSet())
            .apply()
    }

    /**
     * Đảm bảo khi chuyển ngày, SharedPreferences cũng được reset.
     */
    private fun clearSkipSetIfNewDay() {
        val prefs = skipPrefs()
        val today = todayString()
        val savedDay = prefs.getString("day", null)
        if (savedDay != today) {
            prefs.edit()
                .putString("day", today)
                .putStringSet("apps", emptySet())
                .apply()
        }
    }
}
