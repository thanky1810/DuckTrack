package com.example.ducktrack.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ducktrack.data.LimitsStore
import com.example.ducktrack.data.UsageRepository
import com.example.ducktrack.utils.msToReadable

class LimitCheckWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    private val repo = UsageRepository(ctx)
    private val store = LimitsStore(ctx)

    override suspend fun doWork(): Result {
        // Map usage theo package thật (iconPackage), fallback về label nếu thiếu
        val today = repo.queryToday().associateBy { it.iconPackage ?: it.packageName }

        val limits = store.getAll() // Map<realPackageName, minutes>
        limits.forEach { (pkg, minutes) ->
            val usedMs = today[pkg]?.totalForegroundMs ?: 0L
            if (usedMs >= minutes * 60_000L) {
                notify(pkg, minutes, usedMs)
            }
        }
        return Result.success()
    }

    private fun notify(pkg: String, limitMin: Int, usedMs: Long) {
        // Android 13+: nếu chưa có quyền thì KHÔNG gửi
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val nm = NotificationManagerCompat.from(applicationContext)
        val channelId = "limit"

        if (Build.VERSION.SDK_INT >= 26) {
            val sys = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(
                channelId,
                "Giới hạn",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            sys.createNotificationChannel(ch)
        }

        val text = "Đã vượt giới hạn $limitMin phút cho $pkg (dùng ${msToReadable(usedMs)})."
        val notif = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Vượt giới hạn")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .build()

        nm.notify(pkg.hashCode(), notif)
    }
}
