// app/src/main/java/com/example/ducktrack/data/UsageRepository.kt
package com.example.ducktrack.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.example.ducktrack.data.model.AppUsage
import com.example.ducktrack.utils.usageManager
import java.util.Calendar
import kotlin.math.max

class UsageRepository(private val ctx: Context) {

    /* =========================================================
       1) Gom tên hiển thị (Facebook/Zalo/TikTok/Telegram…)
       ========================================================= */
    private val CANONICAL_MAP: List<Pair<(String) -> Boolean, String>> = listOf(
        ({ p: String -> p.startsWith("com.facebook.") } to "Facebook"),
        ({ p: String -> p.startsWith("com.instagram.") } to "Instagram"),
        ({ p: String -> p == "com.zing.zalo" } to "Zalo"),
        ({ p: String -> p.startsWith("org.telegram.") || p == "it.tdlight.client" } to "Telegram"),
        ({ p: String -> p.startsWith("com.ss.android.ugc.") || p == "com.zhiliaoapp.musically" } to "TikTok"),
        ({ p: String -> p == "com.skype.raider" } to "Skype"),
        ({ p: String -> p == "com.google.android.youtube" } to "YouTube"),
        ({ p: String -> p == "com.android.chrome" } to "Chrome")
    )

    private fun canonicalNameOf(pkg: String, fallbackLabel: String): String {
        val hit = CANONICAL_MAP.firstOrNull { (pred, _) -> pred(pkg) }?.second
        return hit ?: fallbackLabel
    }

    /* =========================================================
       2) Bộ lọc hiển thị
       - KHÔNG kiểm tra launcher (nhiều máy Samsung trả về rỗng)
       - Whitelist các app phổ biến
       - Ẩn một số app hệ thống chắc chắn không cần hiện
       ========================================================= */
    private val ALWAYS_ALLOW = setOf(
        // MXH phổ biến & biến thể
        "com.facebook.katana", "com.facebook.orca", "com.facebook.lite",
        "com.instagram.android",
        "com.zing.zalo",
        "org.telegram.messenger", "org.telegram.messenger.web", "org.telegram.plus", "it.tdlight.client",
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme",
        "com.skype.raider",
        "com.google.android.youtube",
        "com.android.chrome"
    )

    private val DENY_EXACT = setOf(
        "com.android.settings",
        "com.google.android.apps.nexuslauncher", "com.miui.home", "com.oppo.launcher",
        "com.huawei.android.launcher", "com.vivo.launcher", "com.samsung.android.launcher",
        "com.android.camera", "com.google.android.apps.photos", "com.google.android.apps.files",
        "com.google.android.calculator", "com.google.android.calendar",
        "com.google.android.dialer", "com.android.contacts"
    )

    private fun isDisplayable(pm: PackageManager, pkg: String): Boolean {
        if (ALWAYS_ALLOW.contains(pkg)) return true
        if (DENY_EXACT.contains(pkg)) return false

        // Nếu lấy được nhãn ứng dụng => cho hiển thị
        val hasLabel = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString().isNotBlank()
        }.getOrDefault(false)
        return hasLabel
    }

    /* =========================================================
       3) Cách 1: Lấy nhanh từ UsageStats (có thể thiếu trên 1 số ROM)
       ========================================================= */
    @Suppress("DEPRECATION")
    private fun computeByUsageStats(start: Long, end: Long): Map<String, Long> {
        val pm = ctx.packageManager
        val usm: UsageStatsManager = usageManager(ctx)
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end).orEmpty()

        val acc = HashMap<String, Long>()
        stats.forEach { s ->
            val pkg = s.packageName
            if (!isDisplayable(pm, pkg)) return@forEach
            val ms = max(0L, s.totalTimeInForeground)
            if (ms <= 0L) return@forEach
            acc[pkg] = (acc[pkg] ?: 0L) + ms
        }
        return acc
    }

    /* =========================================================
       4) Cách 2 (fallback): Tự tính từ UsageEvents
       Bắt sự kiện RESUMED/PAUSED + FOREGROUND/BACKGROUND
       ========================================================= */
    private fun computeByEvents(start: Long, end: Long): Map<String, Long> {
        val pm = ctx.packageManager
        val usm: UsageStatsManager = usageManager(ctx)
        val events = usm.queryEvents(start, end)

        val acc = HashMap<String, Long>()
        var currentPkg: String? = null
        var currentTs: Long = start

        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val type = e.eventType
            val pkg = e.packageName ?: continue
            val ts = e.timeStamp

            val isResume = (type == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    type == UsageEvents.Event.ACTIVITY_RESUMED)
            val isPause = (type == UsageEvents.Event.MOVE_TO_BACKGROUND ||
                    type == UsageEvents.Event.ACTIVITY_PAUSED)

            if (isResume) {
                // đóng phiên cũ (nếu có)
                if (currentPkg != null && isDisplayable(pm, currentPkg!!)) {
                    acc[currentPkg!!] = (acc[currentPkg!!] ?: 0L) + (ts - currentTs)
                }
                currentPkg = pkg
                currentTs = ts
            } else if (isPause && currentPkg == pkg) {
                if (isDisplayable(pm, pkg)) {
                    acc[pkg] = (acc[pkg] ?: 0L) + (ts - currentTs)
                }
                currentPkg = null
            }
        }
        // đóng phiên còn treo đến cuối khoảng
        if (currentPkg != null && isDisplayable(pm, currentPkg!!)) {
            acc[currentPkg!!] = (acc[currentPkg!!] ?: 0L) + (end - currentTs)
        }
        return acc
    }

    /* =========================================================
       5) API public: thống kê hôm nay, gộp theo tên chuẩn hoá
       ========================================================= */
    fun queryToday(): List<AppUsage> {
        val pm = ctx.packageManager
        val end = System.currentTimeMillis()
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // (a) Thử UsageStats trước
        var raw = computeByUsageStats(start, end)

        // (b) Nếu đáng ngờ (chỉ 1–2 app như DuckTrack/Camera), fallback qua Events
        val totalApps = raw.keys.size
        val totalMs = raw.values.sum()
        if (totalApps <= 2 || totalMs <= 60_000L) { // <= 1 phút tổng thì coi như rỗng
            raw = computeByEvents(start, end)
        }

        // Gộp theo tên chuẩn hoá và lấy gói đại diện để hiện icon
        data class Agg(var ms: Long = 0L, var iconPkg: String? = null, var label: String = "")
        val agg = HashMap<String, Agg>()
        raw.forEach { (pkg, ms) ->
            val realLabel = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault(pkg)
            val name = canonicalNameOf(pkg, realLabel) // → Facebook/TikTok/…
            val a = agg.getOrPut(name) { Agg(label = name) }
            a.ms += ms
            if (a.iconPkg == null) a.iconPkg = pkg
        }

        return agg.entries
            .map { (name, a) ->
                AppUsage(
                    packageName = name,
                    label = name,
                    totalForegroundMs = a.ms,
                    iconPackage = a.iconPkg
                )
            }
            .filter { it.totalForegroundMs > 0L }
            .sortedByDescending { it.totalForegroundMs }
            .take(30)
    }
}
