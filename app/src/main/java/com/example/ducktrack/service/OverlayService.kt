package com.example.ducktrack.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
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
import androidx.core.app.NotificationCompat
import com.example.ducktrack.R
import com.example.ducktrack.utils.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class OverlayService : Service() {

    private var overlayView: FrameLayout? = null

    private val windowManager by lazy {
        getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private var currentPackageName: String? = null
    private var currentLimitMinutes: Int? = null

    // --- PHẦN MỚI: QUẢN LÝ TRẠNG THÁI SERVICE ---
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        // Cập nhật trạng thái đang chạy
        _isServiceRunning.value = true

        // Bắt buộc chạy Foreground để không bị hệ thống kill
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        // Cập nhật trạng thái đã tắt
        _isServiceRunning.value = false
        hideBlockScreen()
    }
    // ---------------------------------------------

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

    @SuppressLint("InflateParams")
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
            @SuppressLint("SetTextI18n")
            txtLimit.text =
                "🔒 Đang trong chế độ SIÊU TẬP TRUNG!\nBạn không thể sử dụng ứng dụng này."
            txtLimit.setTextColor(Color.RED)

            btnExtend.visibility = View.GONE
            btnRemove.visibility = View.GONE
        } else {
            // Chế độ thường
            @SuppressLint("SetTextI18n")
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
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS  or
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
        // Gửi lệnh sang UsageMonitorService (Service đếm giờ)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun handleRemoveLimit() {
        val pkg = currentPackageName ?: return
        val intent = Intent(this, UsageMonitorService::class.java).apply {
            action = UsageMonitorService.ACTION_REMOVE_LIMIT
            putExtra("packageName", pkg)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
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

    // --- TẠO NOTIFICATION CHO FOREGROUND SERVICE ---
    private fun createNotification(): Notification {
        val channelId = "OverlayServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "DuckTrack Overlay",
                NotificationManager.IMPORTANCE_LOW // Low để không kêu ting ting liên tục
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("DuckTrack đang chạy")
            .setContentText("Dịch vụ hiển thị màn hình chặn đang hoạt động")
            .setSmallIcon(R.drawable.duck_waiting) // Đảm bảo bạn có icon này
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- COMPANION OBJECT: SỬA LỖI CHO SETTINGS VIEW MODEL ---
    companion object {
        const val TAG = "OverlayService"
        const val ACTION_SHOW_BLOCK = "SHOW_BLOCK"
        const val ACTION_HIDE_BLOCK = "HIDE_BLOCK"
        private const val NOTIFICATION_ID = 999

        // Biến StateFlow để SettingsViewModel lắng nghe
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning = _isServiceRunning.asStateFlow()

        // Hàm Start Service (Dùng trong SettingsViewModel)
        fun startService(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        // Hàm Stop Service (Dùng trong SettingsViewModel)
        fun stopService(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.stopService(intent)
        }
    }
}