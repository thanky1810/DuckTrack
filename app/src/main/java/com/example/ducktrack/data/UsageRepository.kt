package com.example.ducktrack.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.ducktrack.data.model.AppUsage
import com.example.ducktrack.utils.usageManager
import java.util.Calendar
import kotlin.math.max

class UsageRepository(private val ctx: Context) {

    private val TAG = "UsageRepository"
    private val labelCache = HashMap<String, String>()

    /* =========================================================
       1. TỰ ĐỘNG LẤY TÊN APP
       ========================================================= */
    private fun getAppLabel(pm: PackageManager, packageName: String): String {
        if (labelCache.containsKey(packageName)) {
            return labelCache[packageName]!!
        }
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(appInfo).toString()
            labelCache[packageName] = label
            label
        } catch (_: Exception) {
            packageName
        }
    }

    /* =========================================================
       2. TỰ ĐỘNG LỌC APP (Chỉ lấy app người dùng mở được)
       ========================================================= */
    private fun isUserApp(pm: PackageManager, packageName: String): Boolean {
        if (packageName.startsWith("com.android.") ||
            packageName.startsWith("com.google.android.overlay") ||
            packageName.contains("nexuslauncher") ||
            packageName.contains("systemui")
        ) {
            return false
        }
        return pm.getLaunchIntentForPackage(packageName) != null
    }

    /* =========================================================
       3. LOGIC TÍNH TOÁN (ĐÃ TỐI ƯU ĐỂ KHÔNG TÍNH CHẠY NGẦM)
       ========================================================= */

    // Cách 1: Dùng UsageStats có sẵn (Nhanh)
    private fun computeByUsageStats(start: Long, end: Long): Map<String, Long> {
        val pm = ctx.packageManager
        val usm = usageManager(ctx)
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end).orEmpty()

        val acc = HashMap<String, Long>()

        for (s in stats) {
            if (s.lastTimeUsed < start) continue

            val pkg = s.packageName
            val ms = max(0L, s.totalTimeInForeground)

            if (ms > 0 && isUserApp(pm, pkg)) {
                acc[pkg] = (acc[pkg] ?: 0L) + ms
            }
        }
        return acc
    }

    // Cách 2: Tính thủ công bằng Events (Chính xác tuyệt đối 100%)
    // Chỉ tính khi màn hình sáng và app đang hiện
    private fun computeByEvents(start: Long, end: Long): Map<String, Long> {
        val pm = ctx.packageManager
        val usm = usageManager(ctx)
        val events = usm.queryEvents(start, end)

        val acc = HashMap<String, Long>()
        val startTimes = HashMap<String, Long>()

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName

            if (!isUserApp(pm, pkg)) continue

            if (event.eventType == 1) {
                // App hiện lên màn hình -> Bắt đầu tính giờ
                startTimes[pkg] = event.timeStamp
            } else if (event.eventType == 1) {
                // App ẩn đi -> Kết thúc tính giờ
                val startTime = startTimes[pkg]
                if (startTime != null) {
                    val duration = event.timeStamp - startTime
                    if (duration > 0) {
                        acc[pkg] = (acc[pkg] ?: 0L) + duration
                    }
                    // Xóa để chờ lần mở tiếp theo
                    startTimes.remove(pkg)
                }
            }
        }

        // Xử lý trường hợp App đang mở ngay lúc này (chưa có sự kiện đóng)
        for ((pkg, startTime) in startTimes) {
            val duration = end - startTime
            if (duration > 0) {
                acc[pkg] = (acc[pkg] ?: 0L) + duration
            }
        }

        return acc
    }

    /* =========================================================
       4. HÀM QUERY CHO TRANG CHỦ (MAIN SCREEN)
       ========================================================= */
    fun queryUsageForDate(dateTimestamp: Long): List<AppUsage> {
        val pm = ctx.packageManager

        // Xác định đầu ngày và cuối ngày
        val startCalendar = Calendar.getInstance().apply {
            timeInMillis = dateTimestamp
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(
            Calendar.MILLISECOND,
            0
        )
        }
        val startTime = startCalendar.timeInMillis

        val endCalendar = Calendar.getInstance().apply {
            timeInMillis = dateTimestamp
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(
            Calendar.MILLISECOND,
            999
        )
        }

        // Không lấy tương lai
        val now = System.currentTimeMillis()
        val endTime = if (endCalendar.timeInMillis > now) now else endCalendar.timeInMillis

        // Ưu tiên cách 1, nếu dữ liệu ít (chưa cập nhật kịp) thì dùng cách 2
        var rawMap = computeByUsageStats(startTime, endTime)
        val totalMs = rawMap.values.sum()

        if (rawMap.isEmpty() || totalMs < 60000) {
            // Nếu tổng thời gian < 1 phút, có thể hệ thống chưa update -> Dùng Events
            Log.d(TAG, "Fallback to Events for date: $dateTimestamp")
            rawMap = computeByEvents(startTime, endTime)
        }

        return rawMap.map { (pkg, ms) ->
            AppUsage(
                packageName = pkg,
                label = getAppLabel(pm, pkg),
                totalForegroundMs = ms,
                iconPackage = pkg
            )
        }.sortedByDescending { it.totalForegroundMs }
    }

    // Hàm hỗ trợ cũ
    fun queryToday(): List<AppUsage> {
        return queryUsageForDate(System.currentTimeMillis())
    }

    /* =========================================================
       5. CÁC HÀM THỐNG KÊ BIỂU ĐỒ
       ========================================================= */

    fun getMonthlyStats(startTime: Long, endTime: Long): Map<String, Long> {
        val usm = usageManager(ctx)
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_MONTHLY, startTime, endTime)
        val resultMap = LinkedHashMap<String, Long>()
        val calendar = Calendar.getInstance()

        if (stats != null) {
            for (usage in stats) {
                if (usage.totalTimeInForeground > 0) {
                    calendar.timeInMillis = usage.firstTimeStamp
                    val month = calendar.get(Calendar.MONTH) + 1
                    val key = "T$month"
                    val current = resultMap[key] ?: 0L
                    resultMap[key] = current + usage.totalTimeInForeground
                }
            }
        }
        return resultMap
    }

    fun getTopAppsStats(startTime: Long, endTime: Long): Map<String, Long> {
        val usm = usageManager(ctx)
        val statsMap = usm.queryAndAggregateUsageStats(startTime, endTime)
        val pm = ctx.packageManager
        val resultMap = HashMap<String, Long>()

        for ((pkg, usage) in statsMap) {
            if (usage.totalTimeInForeground > 0 && isUserApp(pm, pkg)) {
                val label = getAppLabel(pm, pkg)
                val current = resultMap[label] ?: 0L
                resultMap[label] = current + usage.totalTimeInForeground
            }
        }
        return resultMap
    }
}