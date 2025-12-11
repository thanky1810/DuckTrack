package com.example.ducktrack

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.work.*
import com.example.ducktrack.ui.AppRoot.AppRoot
import com.example.ducktrack.ui.theme.DuckTrackTheme
import com.example.ducktrack.worker.LimitCheckWorker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var analytics: FirebaseAnalytics

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(this, gso)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ẩn thanh điều hướng ngay khi mở
        hideSystemUI()

        analytics = Firebase.analytics
        setupLimitCheckWorker()

        // --- KIỂM TRA QUYỀN (QUAN TRỌNG) ---
        // Nếu không có quyền này, AiChatViewModel sẽ không lấy được danh sách App dùng nhiều
        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "Cấp quyền truy cập để Vịt tính năng AI hoạt động nhé!", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        // ------------------------------------

        setContent {
            DuckTrackTheme {
                AppRoot(googleSignInClient = googleSignInClient)
            }
        }
    }

    // --- LOGIC KIỂM TRA QUYỀN ---
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Dành cho Android 10 (API 29) trở lên
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        } else {
            // Dành cho Android 9 trở xuống (API 26-28)
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        }

        return mode == AppOpsManager.MODE_ALLOWED
    }

    // --- LOGIC ẨN THANH ĐIỀU HƯỚNG (CỦA BẠN) ---
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                // CHỈ ẨN THANH ĐIỀU HƯỚNG (Nút Home/Back dưới đáy)
                hide(WindowInsets.Type.navigationBars())

                // KHÔNG ẨN THANH TRẠNG THÁI (Status Bar - Pin/Giờ)
                show(WindowInsets.Type.statusBars())

                // Vuốt lên để hiện lại thanh điều hướng
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android cũ hơn
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // Chỉ ẩn thanh dưới
                    // ĐÃ XÓA: View.SYSTEM_UI_FLAG_FULLSCREEN (Để hiện thanh Pin/Giờ)
                    )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun setupLimitCheckWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val checkWork = PeriodicWorkRequestBuilder<LimitCheckWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "LimitCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            checkWork
        )
    }
}