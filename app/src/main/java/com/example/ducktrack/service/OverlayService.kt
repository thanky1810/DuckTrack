package com.example.ducktrack.service

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
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

    // Lưu app đang bị chặn và limit hiện tại
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

                if (packageName != null && limitMinutes > 0) {
                    showBlockScreen(packageName, limitMinutes)
                } else {
                    Log.w(TAG, "ACTION_SHOW_BLOCK but missing extras")
                }
            }

            ACTION_HIDE_BLOCK -> {
                hideBlockScreen()
            }

            else -> {
                Log.w(TAG, "Unknown action: $action")
            }
        }

        return START_STICKY
    }

    /**
     * Hiển thị màn hình chặn full-screen (View XML, không dùng Compose)
     */
    private fun showBlockScreen(packageName: String, limitMinutes: Int) {
        Log.d(TAG, "showBlockScreen for $packageName, limit=$limitMinutes")

        if (!PermissionHelper.hasOverlayPermission(this)) {
            Log.w(TAG, "No overlay permission, cannot show block screen")
            Toast.makeText(
                this,
                "DuckTrack cần quyền hiển thị trên ứng dụng khác để chặn màn hình.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Nếu overlay đang hiển thị rồi thì KHÔNG tạo lại nữa
        if (overlayView != null) {
            Log.d(TAG, "Overlay already visible, skip re-adding")
            return
        }

        // Lưu lại để dùng cho các nút
        currentPackageName = packageName
        currentLimitMinutes = limitMinutes

        // Inflate layout XML
        val inflater = LayoutInflater.from(this)
        val root = inflater.inflate(R.layout.view_block_overlay, null) as FrameLayout
        overlayView = root

        val appName = getAppName(packageName)

        // Set text
        root.findViewById<TextView>(R.id.txtBlockedAppName)?.text = appName
        root.findViewById<TextView>(R.id.txtBlockedLimit)?.text =
            "Giới hạn: $limitMinutes phút/ngày"

        // Nút "Thêm 15 phút"
        root.findViewById<Button>(R.id.btnExtend)?.setOnClickListener {
            handleExtend15Min()
            // Sau khi gửi yêu cầu extend → đóng overlay
            hideBlockScreen()
        }

        // Nút "Xóa giới hạn"
        root.findViewById<Button>(R.id.btnRemoveLimit)?.setOnClickListener {
            handleRemoveLimit()
            // Sau khi xóa giới hạn → đóng overlay
            hideBlockScreen()
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
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(root, layoutParams)
            Log.d(TAG, "Overlay view added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
        }
    }

    /**
     * Lấy tên hiển thị từ package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    /**
     * Nút "Thêm 15 phút" → Gửi ACTION_EXTEND_TIME cho UsageMonitorService
     */
    private fun handleExtend15Min() {
        val pkg = currentPackageName ?: return
        Log.d(TAG, "handleExtend15Min for $pkg")

        val intent = Intent(this, UsageMonitorService::class.java).apply {
            action = UsageMonitorService.ACTION_EXTEND_TIME
            putExtra("packageName", pkg)
        }

        try {
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start UsageMonitorService for EXTEND_TIME", e)
        }
    }

    /**
     * Nút "Xóa giới hạn" → Gửi ACTION_REMOVE_LIMIT cho UsageMonitorService
     */
    private fun handleRemoveLimit() {
        val pkg = currentPackageName ?: return
        Log.d(TAG, "handleRemoveLimit for $pkg")

        val intent = Intent(this, UsageMonitorService::class.java).apply {
            action = UsageMonitorService.ACTION_REMOVE_LIMIT
            putExtra("packageName", pkg)
        }

        try {
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start UsageMonitorService for REMOVE_LIMIT", e)
        }
    }

    /**
     * Ẩn overlay nếu đang hiển thị
     */
    private fun hideBlockScreen() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
                Log.d(TAG, "Overlay view removed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay view", e)
            } finally {
                overlayView = null
                currentPackageName = null
                currentLimitMinutes = null
            }
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
