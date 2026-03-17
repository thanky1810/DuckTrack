package com.example.ducktrack.data

import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class DataAggregator(private val context: Context) {

    // Kết nối Database
    private val db = AppDatabase.getDatabase(context)

    // 1. DỮ LIỆU SỬ DỤNG ĐIỆN THOẠI (SCREEN TIME)
    fun getScreenTimeSummary(): String {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(
            Calendar.SECOND,
            0
        )
        val startTime = calendar.timeInMillis

        val usageStatsList =
            usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        if (usageStatsList.isNullOrEmpty()) return "Không có dữ liệu sử dụng màn hình (hoặc chưa cấp quyền)."

        val appUsageMap = HashMap<String, Long>()
        for (usage in usageStatsList) {
            if (usage.totalTimeInForeground > 0) {
                appUsageMap[usage.packageName] =
                    appUsageMap.getOrDefault(usage.packageName, 0L) + usage.totalTimeInForeground
            }
        }

        val sortedList = appUsageMap.entries.sortedByDescending { it.value }.take(5)
        val pm = context.packageManager
        val sb = StringBuilder()
        var totalTime = 0L

        for ((pkg, time) in sortedList) {
            totalTime += time
            val appName = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (_: Exception) {
                pkg
            }
            val minutes = time / 1000 / 60
            if (minutes > 0) sb.append("- $appName: $minutes phút\n")
        }

        val totalHours = totalTime / 1000 / 60 / 60
        val totalMins = (totalTime / 1000 / 60) % 60

        return "Tổng on-screen hôm nay: ${totalHours}h ${totalMins}p.\nTop App:\n$sb"
    }

    // 2. DỮ LIỆU CÂY TRỒNG (TOÀN BỘ LỊCH SỬ)
    suspend fun getTreeSummary(): String = withContext(Dispatchers.IO) {
        try {
            val grownTrees = db.userDao().getAllGrownTreesList()

            if (grownTrees.isEmpty()) {
                return@withContext "Chưa trồng được cây nào."
            }

            val totalTrees = grownTrees.size
            // Ước tính thời gian tập trung (mỗi cây ~ 25 phút)
            val estimatedFocusTime = totalTrees * 25
            val hours = estimatedFocusTime / 60
            val mins = estimatedFocusTime % 60

            """
            - Tổng số cây đã trồng: $totalTrees cây.
            - Ước tính thời gian tập trung tích lũy: ${hours}h ${mins}p.
            """.trimIndent()
        } catch (e: Exception) {
            "Lỗi đọc dữ liệu cây: ${e.message}"
        }
    }

    // 3. DỮ LIỆU NHIỆM VỤ (ĐÃ SỬA: DÙNG .description)
    suspend fun getTaskSummary(): String = withContext(Dispatchers.IO) {
        try {
            val allTasks = db.userDao().getAllTasksList()

            if (allTasks.isEmpty()) {
                return@withContext "Chưa có nhiệm vụ nào."
            }

            val completedCount = allTasks.count { it.isCompleted }
            val pendingCount = allTasks.count { !it.isCompleted }

            // Lấy nội dung 3 nhiệm vụ chưa làm gần nhất
            val pendingNames = allTasks
                .filter { !it.isCompleted }
                .take(3)
                .joinToString(", ") {
                    // [ĐÃ SỬA] Dùng it.description thay vì it.title
                    it.description
                }

            """
            - Đã hoàn thành: $completedCount nhiệm vụ.
            - Còn lại: $pendingCount nhiệm vụ chưa xong.
            - Việc cần làm ngay: ${pendingNames.ifEmpty { "Đã xong hết!" }}
            """.trimIndent()
        } catch (e: Exception) {
            "Lỗi đọc dữ liệu task: ${e.message}"
        }
    }

    // TỔNG HỢP TOÀN BỘ
    suspend fun getFullDailyReport(): String {
        val screen = getScreenTimeSummary()
        val trees = getTreeSummary()
        val tasks = getTaskSummary()

        return """
            [DỮ LIỆU THỰC TẾ CỦA NGƯỜI DÙNG]
            
            1. SỬ DỤNG ĐIỆN THOẠI (HÔM NAY):
            $screen
            
            2. THÀNH TÍCH TẬP TRUNG (LỊCH SỬ):
            $trees
            
            3. TIẾN ĐỘ CÔNG VIỆC:
            $tasks
        """.trimIndent()
    }
}