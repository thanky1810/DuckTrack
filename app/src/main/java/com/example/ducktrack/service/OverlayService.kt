package com.example.ducktrack.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import com.example.ducktrack.ui.components.BlockOverlayScreen

/**
 * Service hiển thị overlay chặn app khi vượt giới hạn
 */
class OverlayService : Service() {

    private val CHANNEL_ID = "overlay_channel"
    private val NOTIFICATION_ID = 1002

    private var overlayView: FrameLayout? = null
    private var windowManager: WindowManager? = null

    private val showOverlay = mutableStateOf(false)
    private var currentPackageName = ""
    private var currentLimitMinutes = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_BLOCK -> {
                currentPackageName = intent.getStringExtra("packageName") ?: ""
                currentLimitMinutes = intent.getIntExtra("limitMinutes", 0)
                showBlockScreen()
            }
            ACTION_HIDE_BLOCK -> hideBlockScreen()
        }
        return START_NOT_STICKY
    }

    private fun showBlockScreen() {
        if (overlayView != null) return // Đã hiển thị rồi

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        overlayView = FrameLayout(this).apply {
            addView(ComposeView(context).apply {
                setContent {
                    BlockOverlayScreen(
                        appName = getAppName(currentPackageName),
                        limitMinutes = currentLimitMinutes,
                        onExtend15Min = {
                            handleExtend15Min()
                            hideBlockScreen()
                        },
                        onRemoveLimit = {
                            handleRemoveLimit()
                            hideBlockScreen()
                        }
                    )
                }
            })
        }

        windowManager?.addView(overlayView, params)
    }

    private fun hideBlockScreen() {
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
        stopSelf()
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun handleExtend15Min() {
        val intent = Intent(this, UsageMonitorService::class.java).apply {
            action = UsageMonitorService.ACTION_EXTEND_TIME
            putExtra("packageName", currentPackageName)
        }
        startService(intent)
    }

    private fun handleRemoveLimit() {
        val intent = Intent(this, UsageMonitorService::class.java).apply {
            action = UsageMonitorService.ACTION_REMOVE_LIMIT
            putExtra("packageName", currentPackageName)
        }
        startService(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Block Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Giới hạn thời gian")
            .setContentText("Đang hiển thị cảnh báo")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideBlockScreen()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_SHOW_BLOCK = "SHOW_BLOCK"
        const val ACTION_HIDE_BLOCK = "HIDE_BLOCK"
    }
}