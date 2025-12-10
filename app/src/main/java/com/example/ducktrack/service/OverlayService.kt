package com.example.ducktrack.service

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.example.ducktrack.R
import com.example.ducktrack.utils.PermissionHelper

class OverlayService : Service() {

    private var overlayView: FrameLayout? = null

    private val windowManager by lazy {
        getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private var currentPackageName: String? = null
    private var currentLimitMinutes: Int? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action=$action")

        when (action) {
            ACTION_SHOW_BLOCK -> {
                val packageName = intent.getStringExtra("packageName")
                val limitMinutes = intent.getIntExtra("limitMinutes", -1)
                // Nhận thêm cờ Deep Focus từ Service giám sát
                val isDeepFocus = intent.getBooleanExtra("isDeepFocus", false)

                if (packageName != null) {
                    showBlockScreen(packageName, limitMinutes, isDeepFocus)
                } else {
                    Log.w(TAG, "ACTION_SHOW_BLOCK but missing packageName")
                }
            }

            ACTION_HIDE_BLOCK -> {
                hideBlockScreen()
            }
        }

        return START_STICKY
    }

    private fun showBlockScreen(packageName: String, limitMinutes: Int, isDeepFocus: Boolean) {
        if (!PermissionHelper.hasOverlayPermission(this)) return
        if (overlayView != null) return

        currentPackageName = packageName
        currentLimitMinutes = limitMinutes

        val inflater = LayoutInflater.from(this)
        val root = inflater.inflate(R.layout.view_block_overlay, null) as FrameLayout
        overlayView = root

        val appName = getAppName(packageName)
        val txtAppName = root.findViewById<TextView>(R.id.txtBlockedAppName)
        val txtLimit = root.findViewById<TextView>(R.id.txtBlockedLimit)
        val btnExtend = root.findViewById<Button>(R.id.btnExtend)
        val btnRemove = root.findViewById<Button>(R.id.btnRemoveLimit)

        txtAppName.text = appName

        // --- XỬ LÝ GIAO DIỆN THEO CHẾ ĐỘ ---
        if (isDeepFocus) {
            // Chế độ Siêu Tập Trung: Chặn cứng, chữ đỏ, ẩn nút thoát
            txtLimit.text = "🔒 Đang trong chế độ SIÊU TẬP TRUNG!\nBạn không thể sử dụng ứng dụng này."
            txtLimit.setTextColor(Color.RED)

            btnExtend.visibility = View.GONE
            btnRemove.visibility = View.GONE
        } else {
            // Chế độ thường
            txtLimit.text = "Giới hạn: $limitMinutes phút/ngày"
            // Reset màu về mặc định (thường là trắng hoặc theo theme)
            txtLimit.setTextColor(Color.WHITE)

            btnExtend.visibility = View.VISIBLE
            btnRemove.visibility = View.VISIBLE

            btnExtend.setOnClickListener {
                handleExtend15Min()
                hideBlockScreen()
            }

            btnRemove.setOnClickListener {
                handleRemoveLimit()
                hideBlockScreen()
            }
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }

        try {
            windowManager.addView(root, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun handleExtend15Min() {
        val pkg = currentPackageName ?: return
        val intent = Intent(this, UsageMonitorService::class.java).apply {
            action = UsageMonitorService.ACTION_EXTEND_TIME
            putExtra("packageName", pkg)
        }
        try { startService(intent) } catch (e: Exception) { e.printStackTrace() }
    }

    private fun handleRemoveLimit() {
        val pkg = currentPackageName ?: return
        val intent = Intent(this, UsageMonitorService::class.java).apply {
            action = UsageMonitorService.ACTION_REMOVE_LIMIT
            putExtra("packageName", pkg)
        }
        try { startService(intent) } catch (e: Exception) { e.printStackTrace() }
    }

    private fun hideBlockScreen() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                overlayView = null
                currentPackageName = null
                currentLimitMinutes = null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideBlockScreen()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val TAG = "OverlayService"
        const val ACTION_SHOW_BLOCK = "SHOW_BLOCK"
        const val ACTION_HIDE_BLOCK = "HIDE_BLOCK"
    }
}