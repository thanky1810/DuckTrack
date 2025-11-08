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

class MainActivity : ComponentActivity() {

    // Khởi tạo GoogleSignInClient
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // Yêu cầu ID Token để Firebase xác thực
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(this, gso)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupLimitCheckWorker()

        setContent {
            DuckTrackTheme {
                // Truyền GoogleSignInClient vào AppRoot
                AppRoot(googleSignInClient = googleSignInClient)
            }
        }
    }

    private fun setupLimitCheckWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val checkWork = PeriodicWorkRequestBuilder<LimitCheckWorker>(
            repeatInterval = 15, // Chạy mỗi 15 phút
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