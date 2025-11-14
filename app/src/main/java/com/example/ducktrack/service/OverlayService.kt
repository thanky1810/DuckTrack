package com.example.ducktrack.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.example.ducktrack.ui.components.BlockOverlayScreen

/**
 * Service hiển thị overlay chặn app khi vượt giới hạn
 */
class OverlayService : Service() {

    private var overlayView: FrameLayout? = null
    private var windowManager: WindowManager? = null

    private var currentPackageName = ""
    private var currentLimitMinutes = 0

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action=${intent?.action}")
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
        if (overlayView != null) {
            Log.d(TAG, "Overlay already shown, skip")
            return
        }

        Log.d(TAG, "showBlockScreen for $currentPackageName, limit=$currentLimitMinutes")

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val flags =
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        overlayView = FrameLayout(this).apply {
            val composeView = ComposeView(context).apply {
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
            }
            addView(composeView)
        }

        try {
            windowManager?.addView(overlayView, params)
            Log.d(TAG, "Overlay view added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
        }
    }

    private fun hideBlockScreen() {
        Log.d(TAG, "hideBlockScreen")
        overlayView?.let {
            try {
                windowManager?.removeView(it)
                Log.d(TAG, "Overlay view removed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay view", e)
            }
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
        Log.d(TAG, "handleExtend15Min for $currentPackageName")
        val intent = Intent(this, UsageMonitorService::class.java).apply {
            action = UsageMonitorService.ACTION_EXTEND_TIME
            putExtra("packageName", currentPackageName)
        }
        try {
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start UsageMonitorService for EXTEND_TIME", e)
        }
    }

    private fun handleRemoveLimit() {
        Log.d(TAG, "handleRemoveLimit for $currentPackageName")
        val intent = Intent(this, UsageMonitorService::class.java).apply {
            action = UsageMonitorService.ACTION_REMOVE_LIMIT
            putExtra("packageName", currentPackageName)
        }
        try {
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start UsageMonitorService for REMOVE_LIMIT", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        hideBlockScreen()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val TAG = "OverlayService"
        const val ACTION_SHOW_BLOCK = "SHOW_BLOCK"
        const val ACTION_HIDE_BLOCK = "HIDE_BLOCK"
    }
}
