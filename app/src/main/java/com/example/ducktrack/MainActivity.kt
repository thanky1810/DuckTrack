package com.example.ducktrack

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ducktrack.ui.AppRoot.AppRoot
import com.example.ducktrack.ui.theme.DuckTrackTheme
import com.example.ducktrack.worker.LimitCheckWorker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import java.util.concurrent.TimeUnit

// --- THÊM CÁC IMPORT NÀY (Theo ảnh 2) ---
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.Firebase
// ----------------------------------------

class MainActivity : ComponentActivity() {

    // --- THÊM BIẾN NÀY (Theo ảnh 2) ---
    private lateinit var analytics: FirebaseAnalytics
    // ----------------------------------

    // Khởi tạo GoogleSignInClient (Giữ nguyên)
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

        // --- KHỞI TẠO ANALYTICS (Theo ảnh 2) ---
        // Lấy thực thể FirebaseAnalytics
        analytics = Firebase.analytics
        // --------------------------------------

        setupLimitCheckWorker()

        setContent {
            DuckTrackTheme {
                AppRoot(googleSignInClient = googleSignInClient)
            }
        }
    }

    // (Hàm setupLimitCheckWorker giữ nguyên)
    private fun setupLimitCheckWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val checkWork = PeriodicWorkRequestBuilder<LimitCheckWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
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