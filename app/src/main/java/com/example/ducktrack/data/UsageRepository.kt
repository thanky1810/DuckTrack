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
    // Cache tên app để load nhanh hơn, không phải hỏi hệ thống liên tục
    private val labelCache = HashMap<String, String>()

    /* =========================================================
       1. TỰ ĐỘNG LẤY TÊN APP (Không nhập thủ công)
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
        } catch (e: Exception) {
            packageName // Nếu không tìm thấy tên thì hiện package name
        }
    }

    /* =========================================================
       2. TỰ ĐỘNG LỌC APP
       Logic: Chỉ hiện những app mà người dùng có thể mở (có icon ngoài màn hình)
       ========================================================= */
    private fun isUserApp(pm: PackageManager, packageName: String): Boolean {
        // Loại bỏ nhanh các gói hệ thống/giao diện gây nhiễu
        if (packageName.startsWith("com.android.") ||
            packageName.startsWith("com.google.android.overlay") ||
            packageName.contains("nexuslauncher") ||
            packageName.contains("systemui")) {
            return false
        }

        // Kiểm tra xem app có thể mở được không (có Launch Intent)
        // Đây là cách chuẩn nhất để biết đâu là App người dùng cài
        return pm.getLaunchIntentForPackage(packageName) != null
    }

    /* =========================================================
       3. LOGIC TÍNH TOÁN (GIỮ NGUYÊN BẢN GỐC CHẠY ĐÚNG)
       ========================================================= */

    // Cách 1: UsageStats (Nhanh, ổn định)
    private fun computeByUsageStats(start: Long, end: Long): Map<String, Long> {
        val pm = ctx.packageManager
        val usm = usageManager(ctx)
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end).orEmpty()

        val acc = HashMap<String, Long>()

        for (s in stats) {
            // Reset 0h: Nếu lần cuối dùng nhỏ hơn mốc 0h sáng nay -> Bỏ qua
            if (s.lastTimeUsed < start) continue

            val pkg = s.packageName
            val ms = max(0L, s.totalTimeInForeground)

            // Áp dụng bộ lọc tự động isUserApp ở đây
            if (ms > 0 && isUserApp(pm, pkg)) {
                acc[pkg] = (acc[pkg] ?: 0L) + ms
            }
        }
        return acc
    }

    // Cách 2: Events (Dự phòng khi cách 1 bị lỗi)
    private fun computeByEvents(start: Long, end: Long): Map<String, Long> {
        val pm = ctx.packageManager
        val usm = usageManager(ctx)
        val events = usm.queryEvents(start, end)

        val acc = HashMap<String, Long>()
        var currentPkg: String? = null
        var currentTs: Long = start
        val e = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val pkg = e.packageName

            // Lọc ngay từ đầu
            if (!isUserApp(pm, pkg)) continue

            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                currentPkg = pkg
                currentTs = e.timeStamp
            } else if (e.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                if (currentPkg == pkg) {
                    acc[pkg] = (acc[pkg] ?: 0L) + (e.timeStamp - currentTs)
                }
                currentPkg = null
            }
        }

        // Xử lý app đang mở
        if (currentPkg != null && isUserApp(pm, currentPkg!!)) {
            acc[currentPkg!!] = (acc[currentPkg!!] ?: 0L) + (end - currentTs)
        }

        return acc
    }

    /* =========================================================
       4. HÀM QUERY THEO NGÀY (Nâng cấp từ queryToday)
       dateTimestamp: Là một mốc thời gian bất kỳ trong ngày muốn xem
       ========================================================= */
    fun queryUsageForDate(dateTimestamp: Long): List<AppUsage> {
        val pm = ctx.packageManager
        val usm = usageManager(ctx)

        // 1. Xác định mốc Bắt đầu (00:00:00) và Kết thúc (23:59:59) của ngày được chọn
        val startCalendar = Calendar.getInstance().apply {
            timeInMillis = dateTimestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = startCalendar.timeInMillis

        val endCalendar = Calendar.getInstance().apply {
            timeInMillis = dateTimestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        // Lưu ý: Nếu xem ngày hôm nay, thì 'end' không được vượt quá giờ hiện tại
        // để tránh sai số liệu tương lai (dù không có data nhưng logic nên chặt chẽ)
        val now = System.currentTimeMillis()
        val endTime = if (endCalendar.timeInMillis > now) now else endCalendar.timeInMillis

        // 2. Thử Cách 1: UsageStats
        var rawMap = computeByUsageStats(startTime, endTime)

        // 3. Nếu dữ liệu quá ít -> Thử Cách 2: Events
        val totalMs = rawMap.values.sum()
        if (rawMap.isEmpty() || totalMs < 60000) {
            // Chỉ fallback nếu là ngày hôm nay hoặc hôm qua (Events thường chỉ lưu vài ngày)
            // Nếu xem quá khứ xa, UsageStats thường ổn định hơn Events đã bị xóa.
            Log.d(TAG, "Fallback to Events for date: $dateTimestamp")
            rawMap = computeByEvents(startTime, endTime)
        }

        // 4. Xuất kết quả
        return rawMap.map { (pkg, ms) ->
            AppUsage(
                packageName = pkg,
                label = getAppLabel(pm, pkg),
                totalForegroundMs = ms,
                iconPackage = pkg
            )
        }
            .sortedByDescending { it.totalForegroundMs }
    }

    // Hàm giữ tương thích cũ (nếu cần), gọi lại hàm trên với thời gian hiện tại
    fun queryToday(): List<AppUsage> {
        return queryUsageForDate(System.currentTimeMillis())
    }

    /* =========================================================
       5. HÀM LẤY THỐNG KÊ TỔNG HỢP CHO BIỂU ĐỒ (MỚI)
       ========================================================= */

    // Lấy tổng thời gian sử dụng theo từng ngày trong khoảng thời gian
    fun getDailyStats(startTime: Long, endTime: Long): Map<String, Long> {
        val usm = usageManager(ctx)
        // Query theo ngày
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        val resultMap = LinkedHashMap<String, Long>()
        val calendar = Calendar.getInstance()

        // Khởi tạo map rỗng cho các ngày để tránh bị thiếu ngày
        // (Logic đơn giản: Group by ngày trong năm)
        if (stats != null) {
            for (usage in stats) {
                if (usage.totalTimeInForeground > 0) {
                    calendar.timeInMillis = usage.firstTimeStamp
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    val month = calendar.get(Calendar.MONTH) + 1
                    val key = "$day/$month"

                    val current = resultMap[key] ?: 0L
                    resultMap[key] = current + usage.totalTimeInForeground
                }
            }
        }
        return resultMap
    }

    // Lấy tổng thời gian theo từng tháng (cho biểu đồ toàn bộ)
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
                    val year = calendar.get(Calendar.YEAR)
                    val key = "T$month" // Ví dụ: T12, T1

                    val current = resultMap[key] ?: 0L
                    resultMap[key] = current + usage.totalTimeInForeground
                }
            }
        }
        return resultMap
    }
    // 3. (MỚI) Top App sử dụng nhiều nhất (Tổng hợp theo App)
    fun getTopAppsStats(startTime: Long, endTime: Long): Map<String, Long> {
        val usm = usageManager(ctx)
        // queryAndAggregateUsageStats: Tự động cộng dồn thời gian cho từng app trong khoảng thời gian
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