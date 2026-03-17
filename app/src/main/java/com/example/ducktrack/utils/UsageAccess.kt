package com.example.ducktrack.utils

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

@Suppress("DEPRECATION")
fun hasUsageAccess(ctx: Context): Boolean {
    val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val op = AppOpsManager.OPSTR_GET_USAGE_STATS
    val uid = android.os.Process.myUid()
    val pkg = ctx.packageName
    val mode = if (Build.VERSION.SDK_INT >= 29) {
        appOps.unsafeCheckOpNoThrow(op, uid, pkg)
    } else {
        appOps.checkOpNoThrow(op, uid, pkg)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun usageManager(ctx: Context): UsageStatsManager =
    ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager