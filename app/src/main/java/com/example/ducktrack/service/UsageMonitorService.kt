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
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsageMonitorService : Service() {

    private val TAG = "UsageMonitorService"
    private val CHANNEL_ID = "usage_monitor_channel"
    private val NOTIFICATION_ID = 1001

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val usedMsMap = mutableMapOf<String, Long>()
    private val lastKnownBaselines = mutableMapOf<String, BaselineInfo>()

    private var lastForegroundPackage: String? = null
    private var lastTickTime: Long = System.currentTimeMillis()
    private var blockedPackage: String? = null
    private var currentDay: String = todayString()
    private val lastExtendRequestTime = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        lastTickTime = System.currentTimeMillis()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
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
            while (isActive) {
                val today = todayString()
                if (today != currentDay) {
                    currentDay = today
                    usedMsMap.clear()
                    lastKnownBaselines.clear()
                    blockedPackage = null
                    clearSkipSetIfNewDay()
                    hideBlockOverlay()
                }

                checkUsageLimits()
                delay(2000)
            }
        }
    }

    private suspend fun checkUsageLimits() {
        try {
            val now = System.currentTimeMillis()

            val prevPackage = lastForegroundPackage
            val delta = now - lastTickTime
            if (prevPackage != null && delta in 0L..60_000L) {
                val old = usedMsMap[prevPackage] ?: 0L
                usedMsMap[prevPackage] = old + delta
            }
            lastTickTime = now

            val fg = getCurrentForegroundAppRaw()
            if (fg != null) lastForegroundPackage = fg
            val currentApp = lastForegroundPackage ?: return

            val limitsStore = LimitsStore(applicationContext)
            val limits = limitsStore.getAll()

            // --- ĐỌC CỜ DEEP FOCUS ---
            val isDeepFocus = limitsStore.isDeepFocusEnabled.first()

            if (limits.isEmpty()) {
                if (blockedPackage != null) {
                    hideBlockOverlay()
                    blockedPackage = null
                }
                return
            }

            // Logic Baseline
            usedMsMap.keys.retainAll(limits.keys)
            val baselines = limitsStore.getAllBaselines()
            for ((pkg, baseline) in baselines) {
                val prev = lastKnownBaselines[pkg]
                if (prev == null || prev.day != baseline.day || prev.baselineMs != baseline.baselineMs) {
                    usedMsMap[pkg] = 0L
                    lastKnownBaselines[pkg] = baseline
                }
            }
            lastKnownBaselines.keys.retainAll(baselines.keys)

            val skipTodaySet = loadSkipSetForToday()
            val hasLimit = limits.containsKey(currentApp)

            if (!hasLimit) {
                if (blockedPackage != null) {
                    hideBlockOverlay()
                    blockedPackage = null
                }
                return
            }

            val limitMinutes = limits[currentApp] ?: return
            val limitMs = limitMinutes * 60_000L
            val totalUsed = usedMsMap[currentApp] ?: 0L

            var shouldBlock = false

            // LOGIC CHẶN MỚI
            if (isDeepFocus) {
                // Đang Deep Focus -> Chặn luôn (bất kể usage)
                shouldBlock = true
            } else {
                // Chế độ thường -> Chặn khi hết giờ và không skip
                if (totalUsed >= limitMs && !skipTodaySet.contains(currentApp)) {
                    shouldBlock = true
                }
            }

            if (shouldBlock) {
                if (blockedPackage != currentApp) {
                    // Truyền cờ isDeepFocus sang OverlayService
                    showBlockOverlay(currentApp, limitMinutes, isDeepFocus)
                    blockedPackage = currentApp
                }
            } else {
                if (blockedPackage == currentApp) {
                    hideBlockOverlay()
                    blockedPackage = null
                }
            }

            if (blockedPackage != null && blockedPackage != currentApp) {
                hideBlockOverlay()
                blockedPackage = null
            }

        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun getCurrentForegroundAppRaw(): String? {
        val usm: UsageStatsManager = usageManager(applicationContext)
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 5000
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

    private fun showBlockOverlay(packageName: String, limitMinutes: Int, isDeepFocus: Boolean) {
        if (!android.provider.Settings.canDrawOverlays(applicationContext)) return

        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_BLOCK
            putExtra("packageName", packageName)
            putExtra("limitMinutes", limitMinutes)
            putExtra("isDeepFocus", isDeepFocus) // Gửi cờ
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun hideBlockOverlay() {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_HIDE_BLOCK
        }
        try { startService(intent) } catch (e: Exception) {}
    }

    private fun handleExtendTime(intent: Intent) {
        val packageName = intent.getStringExtra("packageName") ?: return
        val now = System.currentTimeMillis()
        if (now - (lastExtendRequestTime[packageName] ?: 0L) < 3000L) return
        lastExtendRequestTime[packageName] = now

        scope.launch {
            val limitsStore = LimitsStore(applicationContext)
            val all = limitsStore.getAll()
            val currentLimit = all[packageName] ?: return@launch
            val newLimit = currentLimit + 15
            limitsStore.setLimitWithBaseline(packageName, newLimit, baselineMs = 0L)
        }
    }

    private fun handleRemoveLimit(intent: Intent) {
        val packageName = intent.getStringExtra("packageName") ?: return
        scope.launch {
            val set = loadSkipSetForToday()
            if (!set.contains(packageName)) {
                set.add(packageName)
                saveSkipSetToday(set)
            }
            usedMsMap.remove(packageName)
            lastKnownBaselines.remove(packageName)
            if (blockedPackage == packageName) {
                hideBlockOverlay()
                blockedPackage = null
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(CHANNEL_ID, "Usage Monitor", NotificationManager.IMPORTANCE_LOW)
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
        hideBlockOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_MONITORING = "START_MONITORING"
        const val ACTION_STOP_MONITORING = "STOP_MONITORING"
        const val ACTION_EXTEND_TIME = "EXTEND_TIME"
        const val ACTION_REMOVE_LIMIT = "REMOVE_LIMIT"
    }

    private fun todayString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun skipPrefs() = getSharedPreferences("ducktrack_skip_limits", MODE_PRIVATE)

    private fun loadSkipSetForToday(): MutableSet<String> {
        val prefs = skipPrefs()
        val today = todayString()
        val savedDay = prefs.getString("day", null)
        if (savedDay != today) {
            prefs.edit().putString("day", today).putStringSet("apps", emptySet()).apply()
            return mutableSetOf()
        }
        val set = prefs.getStringSet("apps", emptySet()) ?: emptySet()
        return set.toMutableSet()
    }

    private fun saveSkipSetToday(set: Set<String>) {
        skipPrefs().edit().putString("day", todayString()).putStringSet("apps", set.toSet()).apply()
    }

    private fun clearSkipSetIfNewDay() { loadSkipSetForToday() }
}