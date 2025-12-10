package com.example.ducktrack

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
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

    // --- ĐOẠN CODE BẠN VIẾT (GIỮ NGUYÊN) ---
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                hide(WindowInsets.Type.systemBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }
    // ----------------------------------------

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

        // --- [SỬA] THÊM DÒNG NÀY ĐỂ CHẠY HÀM ẨN THANH ĐIỀU HƯỚNG ---
        hideSystemUI()
        // -----------------------------------------------------------

        analytics = Firebase.analytics
        setupLimitCheckWorker()

        setContent {
            DuckTrackTheme {
                AppRoot(googleSignInClient = googleSignInClient)
            }
        }
    }

    // --- [SỬA] THÊM HÀM NÀY ĐỂ ĐẢM BẢO LUÔN ẨN KHI QUAY LẠI APP ---
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }
    // --------------------------------------------------------------

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