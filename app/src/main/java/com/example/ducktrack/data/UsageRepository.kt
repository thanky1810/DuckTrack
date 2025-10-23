// app/src/main/java/com/example/ducktrack/data/UsageRepository.kt
package com.example.ducktrack.data

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.example.ducktrack.data.model.AppUsage
import com.example.ducktrack.utils.usageManager
import java.util.Calendar

class UsageRepository(private val ctx: Context) {

    @Suppress("DEPRECATION") // dùng totalTimeInForeground cho mọi API để tránh lỗi khác phiên bản
    fun queryToday(): List<AppUsage> {
        val pm: PackageManager = ctx.packageManager
        val now = System.currentTimeMillis()

        // mốc 0:00 hôm nay
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val usm: UsageStatsManager = usageManager(ctx)
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            now
        ).orEmpty()

        val grouped = stats.groupBy { it.packageName }.map { (pkg, list) ->
            // CHỈ dùng totalTimeInForeground (deprecated nhưng ổn định)
            // Tránh sumOf + toLong() để không dính overload ambiguity
            val totalMs = list.fold(0L) { acc, u -> acc + u.totalTimeInForeground }

            val label = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (_: Exception) {
                pkg
            }
            AppUsage(
                packageName = pkg,
                label = label,
                totalForegroundMs = totalMs
            )
        }

        return grouped
            .filter { it.totalForegroundMs > 0 }
            .sortedByDescending { it.totalForegroundMs }
            .take(30)
    }
}
