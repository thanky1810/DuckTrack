package com.example.ducktrack.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
<<<<<<< Updated upstream
=======
import android.os.Build
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
    // Lưu trạng thái: packageName -> (startTime, accumulatedMs)
    private val sessionMap = mutableMapOf<String, SessionData>()
    private data class SessionData(var startTime: Long, var accumulatedMs: Long = 0L)

    // Chống double-click "Thêm 15 phút"
=======
    // Map: mỗi app -> tổng ms đã dùng (tự đếm bằng đồng hồ)
    private val usedMsMap = mutableMapOf<String, Long>()

    // Lưu baseline cuối cùng để biết khi nào user SET / ĐỔI limit
    private val lastKnownBaselines = mutableMapOf<String, BaselineInfo>()

    // App foreground ở vòng lặp trước + thời điểm tick trước
    private var lastForegroundPackage: String? = null
    private var lastTickTime: Long = System.currentTimeMillis()

    // Dùng để reset usage khi sang ngày mới
    private var currentDay: String = todayString()

    // Chống spam nút "Extend time"
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
            // Chỉ xử lý nếu app này có giới hạn
=======
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

            // 5) Kiểm tra limit cho app hiện tại
>>>>>>> Stashed changes
            val limitMinutes = limits[currentApp] ?: return
            val limitMs = limitMinutes * 60_000L
            val totalUsed = usedMsMap[currentApp] ?: 0L

<<<<<<< Updated upstream
            // Cập nhật session (hiện tại chủ yếu dùng getTodayUsage)
            val session = sessionMap.getOrPut(currentApp) {
                SessionData(System.currentTimeMillis())
=======
            if (skipTodaySet.contains(currentApp)) {
                Log.i(
                    TAG,
                    "⏭ Bỏ qua giới hạn cho $currentApp trong ngày $currentDay (đã bấm 'Xóa giới hạn' trên màn block)"
                )
                // Không block trong hôm nay, nhưng ngày mai limit hoạt động lại
                return
>>>>>>> Stashed changes
            }

            Log.i(TAG, "🔔 ĐANG GIÁM SÁT: $currentApp")
            Log.i(
                TAG,
                "-> Đã dùng: ${totalUsed / 60000} phút / Giới hạn: $limitMinutes phút (Tổng ms: $totalUsed / $limitMs)"
            )

            // Nếu vượt giới hạn → show overlay
            if (totalUsed >= limitMs) {
                Log.w(TAG, "⚠️⚠️⚠️ VƯỢT GIỚI HẠN: $currentApp. Hiển thị màn hình chặn!")
                showBlockOverlay(currentApp, limitMinutes)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking limits", e)
        }
    }

    /**
<<<<<<< Updated upstream
     * Lấy app đang chạy foreground trong ~5 giây gần nhất
     */
    private fun getCurrentForegroundApp(): String? {
=======
     * Lấy app foreground mới nhất trong 5s gần đây (dựa vào event MOVE_TO_FOREGROUND)
     */
    private fun getCurrentForegroundAppRaw(): String? {
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
     * Lấy tổng thời gian sử dụng app trong ngày hôm nay (từ 0h)
     */
=======
     * Hàm cũ dùng UsageStats để lấy tổng foreground từ 0h đến giờ.
     * Không dùng trong logic chính, nhưng giữ lại nếu cần về sau.
     */
    @Suppress("unused")
>>>>>>> Stashed changes
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

    /**
     * Gửi ACTION_SHOW_BLOCK cho OverlayService
     * ❶ FIX: dùng startService(), KHÔNG dùng startForegroundService()
     *     vì OverlayService hiện tại không gọi startForeground().
     */
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
<<<<<<< Updated upstream
            // DÙNG startService để tránh ForegroundServiceDidNotStartInTimeException
            startService(intent)
=======
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
>>>>>>> Stashed changes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start OverlayService", e)
        }
    }

    /**
     * Xử lý "Thêm 15 phút"
     * - Dùng lastExtendRequestTime để tránh cộng 2 lần trong vài giây
     */
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
<<<<<<< Updated upstream
     * Xử lý "Xóa giới hạn"
=======
     * Nút "Xóa giới hạn" trên MÀN BLOCK:
     *  - KHÔNG xóa limit vĩnh viễn.
     *  - Chỉ đánh dấu: hôm nay bỏ qua giới hạn cho app này.
     *  - Sang ngày mới, limit hoạt động lại như cũ.
     *
     * Xóa VĨNH VIỄN vẫn là nút trong CARD (MainScreen -> vm.removeLimit -> limitsStore.removeLimit).
>>>>>>> Stashed changes
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

    private fun createNotificationChannel() {
<<<<<<< Updated upstream
        val manager = getSystemService(NotificationManager::class.java)
        if (manager == null) return
=======
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
>>>>>>> Stashed changes

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Usage Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Theo dõi thời gian sử dụng ứng dụng"
        }

        manager.createNotificationChannel(channel)
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
<<<<<<< Updated upstream
=======

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
     * Đảm bảo khi chuyển ngày, SharedPreferences cũng đã được reset.
     * (Thực ra loadSkipSetForToday() đã tự xử lý, nhưng thêm hàm này cho rõ ràng intent.)
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
>>>>>>> Stashed changes
}
