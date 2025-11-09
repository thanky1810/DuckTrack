// app/src/main/java/com/example/ducktrack/data/UsageRepository.kt
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
        ({ p: String -> p == "com.android.chrome" } to "Chrome"),

        // === BẮT ĐẦU PHẦN THÊM MỚI (Đổi tên) ===
        ({ p: String -> p == "com.fun.lastwar.vn.gp" } to "Last War VN"),
        ({ p: String -> p == "com.duolingo" } to "Duolingo"),
        ({ p: String -> p == "com.discord" } to "Discord"),
        ({ p: String -> p == "com.android.providers.calendar" } to "Lịch"),
        ({ p: String -> p == "com.android.providers.media" } to "Media Storage")
        // === KẾT THÚC PHẦN THÊM MỚI ===
    )

    private fun canonicalNameOf(pkg: String, fallbackLabel: String): String {
        val hit = CANONICAL_MAP.firstOrNull { (pred, _) -> pred(pkg) }?.second
        return hit ?: fallbackLabel
    }

    /* =========================================================
       2) Bộ lọc hiển thị - CẢI THIỆN
       - Whitelist các app phổ biến
       - GIẢM BỚT blacklist - chỉ ẩn những app thực sự không cần
       - Ưu tiên HIỂN THỊ hơn là ẨN
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
        "com.android.chrome",
        // Thêm một số game phổ biến
        "com.garena.game.kgvn", // Free Fire
        "com.mobile.legends", // Mobile Legends
        "com.tencent.ig", // PUBG Mobile
        "com.miHoYo.GenshinImpact", // Genshin Impact
        "com.dts.freefireth", "com.dts.freefiremax", // Free Fire variants

        // === BẮT ĐẦU PHẦN THÊM MỚI (Cho phép) ===
        "com.android.providers.calendar", // Cho phép Lịch (hệ thống)
        "com.android.providers.media",    // Cho phép Media (hệ thống)
        "com.discord",                    // Cho phép Discord
        "com.duolingo",                   // Cho phép Duolingo
        "com.fun.lastwar.vn.gp"           // Cho phép game Last War
        // === KẾT THÚC PHẦN THÊM MỚI ===
    )

    // CHỈ CHẶN những app THỰC SỰ không cần: Settings, Launcher, System UI
    private val DENY_EXACT = setOf(
        "com.android.settings",
        "com.android.systemui",
        "com.google.android.apps.nexuslauncher", "com.miui.home", "com.oppo.launcher",
        "com.huawei.android.launcher", "com.vivo.launcher", "com.samsung.android.launcher",
        "com.sec.android.app.launcher", "com.android.launcher3"
    )

    // Thêm blacklist theo prefix (thay vì exact match)
    private val DENY_PREFIX = listOf(
        "com.android.internal",
        "com.google.android.gms", // Google Play Services
        "com.google.android.gsf", // Google Services Framework
        "android.", // System packages
    )

    private fun isDisplayable(pm: PackageManager, pkg: String): Boolean {
        // 1. Luôn cho phép whitelist
        if (ALWAYS_ALLOW.contains(pkg)) {
            Log.d(TAG, "✅ ALLOW (whitelist): $pkg")
            return true
        }

        // 2. Chặn exact blacklist
        if (DENY_EXACT.contains(pkg)) {
            Log.d(TAG, "❌ DENY (exact): $pkg")
            return false
        }

        // 3. Chặn theo prefix
        if (DENY_PREFIX.any { pkg.startsWith(it) }) {
            Log.d(TAG, "❌ DENY (prefix): $pkg")
            return false
        }

        // 4. Kiểm tra xem có phải app người dùng cài không
        val isUserApp = try {
            val appInfo = pm.getApplicationInfo(pkg, 0)
            val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            !isSystemApp // Chỉ lấy app người dùng cài
        } catch (e: Exception) {
            false
        }

        // 5. Kiểm tra có label không
        val hasLabel = runCatching {
            val label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            label.isNotBlank()
        }.getOrDefault(false)

        val result = isUserApp && hasLabel
        Log.d(TAG, "${if (result) "✅" else "❌"} $pkg - userApp: $isUserApp, hasLabel: $hasLabel")
        return result
    }

    /* =========================================================
       3) Cách 1: Lấy nhanh từ UsageStats (có thể thiếu trên 1 số ROM)
       ========================================================= */
    @Suppress("DEPRECATION")
    private fun computeByUsageStats(start: Long, end: Long): Map<String, Long> {
        val pm = ctx.packageManager
        val usm: UsageStatsManager = usageManager(ctx)
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end).orEmpty()

        Log.d(TAG, "📊 UsageStats: Found ${stats.size} entries")

        val acc = HashMap<String, Long>()
        stats.forEach { s ->
            val pkg = s.packageName
            val ms = max(0L, s.totalTimeInForeground)

            if (ms > 0L) {
                Log.d(TAG, "  $pkg: ${ms}ms (${ms / 60000}m)")
            }

            if (!isDisplayable(pm, pkg)) return@forEach
            if (ms <= 0L) return@forEach

            acc[pkg] = (acc[pkg] ?: 0L) + ms
        }

        Log.d(TAG, "✅ After filtering: ${acc.size} apps")
        return acc
    }

    /* =========================================================
       4) Cách 2 (fallback): Tự tính từ UsageEvents
       ========================================================= */
    private fun computeByEvents(start: Long, end: Long): Map<String, Long> {
        val pm = ctx.packageManager
        val usm: UsageStatsManager = usageManager(ctx)
        val events = usm.queryEvents(start, end)

        Log.d(TAG, "📊 Using UsageEvents fallback")

        val acc = HashMap<String, Long>()
        var currentPkg: String? = null
        var currentTs: Long = start

        val e = UsageEvents.Event()
        var eventCount = 0

        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            eventCount++

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

        Log.d(TAG, "📊 Processed $eventCount events")

        // đóng phiên còn treo đến cuối khoảng
        if (currentPkg != null && isDisplayable(pm, currentPkg!!)) {
            acc[currentPkg!!] = (acc[currentPkg!!] ?: 0L) + (end - currentTs)
        }

        Log.d(TAG, "✅ After filtering: ${acc.size} apps")
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

        Log.d(TAG, "=== QUERY TODAY: ${java.text.SimpleDateFormat("HH:mm:ss").format(end)} ===")

        // (a) Thử UsageStats trước
        var raw = computeByUsageStats(start, end)

        // (b) Fallback logic - CẢI THIỆN điều kiện
        val totalApps = raw.keys.size
        val totalMs = raw.values.sum()

        Log.d(TAG, "📊 Initial result: $totalApps apps, ${totalMs / 60000}m total")

        // Fallback nếu:
        // - Quá ít app (< 3)
        // - HOẶC tổng thời gian quá ít (< 5 phút)
        if (totalApps < 3 || totalMs < 300_000L) {
            Log.d(TAG, "⚠️ Suspicious data, using Events fallback")
            raw = computeByEvents(start, end)
            Log.d(TAG, "📊 Events result: ${raw.size} apps, ${raw.values.sum() / 60000}m total")
        }

        // Gộp theo tên chuẩn hoá và lấy gói đại diện để hiện icon
        data class Agg(var ms: Long = 0L, var iconPkg: String? = null, var label: String = "")
        val agg = HashMap<String, Agg>()

        raw.forEach { (pkg, ms) ->
            val realLabel = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault(pkg)

            // Đây là nơi tên đẹp được gán (ví dụ: "Last War VN")
            val name = canonicalNameOf(pkg, realLabel) // → Facebook/TikTok/…

            val a = agg.getOrPut(name) { Agg(label = name) }
            a.ms += ms

            // Đây là nơi gói gốc (để lấy icon) được lưu lại
            if (a.iconPkg == null) a.iconPkg = pkg
        }

        val result = agg.entries
            .map { (name, a) ->
                AppUsage(
                    packageName = name,    // Tên gộp (VD: "Last War VN")
                    label = name,          // Tên gộp (VD: "Last War VN")
                    totalForegroundMs = a.ms,
                    iconPackage = a.iconPkg // Gói gốc (VD: "com.fun.lastwar.vn.gp")
                )
            }
            .filter { it.totalForegroundMs > 0L }
            .sortedByDescending { it.totalForegroundMs }
            .take(50) // Tăng từ 30 lên 50 để hiển thị nhiều app hơn

        Log.d(TAG, "✅ FINAL: ${result.size} apps")
        result.take(10).forEach {
            Log.d(TAG, "  ${it.label}: ${it.totalForegroundMs / 60000}m (Icon from: ${it.iconPackage})")
        }

        return result
    }
}