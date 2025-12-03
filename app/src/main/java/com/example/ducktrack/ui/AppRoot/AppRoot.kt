package com.example.ducktrack.ui.AppRoot

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ducktrack.MyApplication
import com.example.ducktrack.data.LimitsStore
import com.example.ducktrack.ui.AuthViewModel
import com.example.ducktrack.ui.introducePage.introduceScreen
import com.example.ducktrack.ui.login.LoginScreen
import com.example.ducktrack.ui.main.MainScreen
import com.example.ducktrack.ui.main.ViewModelFactory
import com.example.ducktrack.ui.main.promodoro.FocusModeScreen
import com.example.ducktrack.ui.main.promodoro.PromodoroViewModel
import com.example.ducktrack.ui.onboarding.OnboardingScreen
import com.example.ducktrack.ui.permission.PermissionScreen
import com.example.ducktrack.ui.splash.SplashScreen
import com.example.ducktrack.ui.theme.DuckTrackTheme
import com.example.ducktrack.utils.hasUsageAccess
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

fun checkUsageAccessPermission(context: Context): Boolean {
    return hasUsageAccess(context)
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppRoot(
    googleSignInClient: GoogleSignInClient
) {
    val appContext = LocalContext.current.applicationContext
    val activityContext = LocalContext.current
    val nav = rememberNavController()

    // --- 1. SETUP DATA STORE & AUTH ---
    val limitsStore = remember { LimitsStore(appContext) }
    val isOnboardingCompleted by limitsStore.onboardingCompleted.collectAsState(initial = null)

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(appContext)
    )

    // Shared ViewModel cho Pomodoro
    val promodoroViewModel: PromodoroViewModel = viewModel(
        factory = ViewModelFactory(appContext as MyApplication)
    )

    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val hasPermission = checkUsageAccessPermission(appContext)

    DuckTrackTheme {
        Scaffold { innerPadding ->
            NavHost(
                navController = nav,
                startDestination = Routes.Splash,
                modifier = Modifier.padding(innerPadding)
            ) {
                // --- MÀN HÌNH SPLASH ---
                composable(Routes.Splash) {
                    SplashScreen(
                        // Truyền trạng thái dữ liệu vào Splash
                        isDataReady = (isOnboardingCompleted != null),
                        onSplashFinished = {
                            // Khi Splash chạy xong Animation + Có dữ liệu
                            val nextRoute = if (isOnboardingCompleted == false) {
                                Routes.Onboarding // Người mới -> Onboarding
                            } else {
                                // Người cũ -> Kiểm tra Đăng nhập & Quyền
                                if (isAuthenticated) {
                                    if (hasPermission) Routes.Main else Routes.Permission
                                } else {
                                    Routes.Home
                                }
                            }

                            // Điều hướng và XÓA Splash khỏi lịch sử (để không Back lại được)
                            nav.navigate(nextRoute) {
                                popUpTo(Routes.Splash) { inclusive = true }
                            }
                        }
                    )
                }

                // --- MÀN HÌNH ONBOARDING ---
                composable(Routes.Onboarding) {
                    OnboardingScreen(
                        onFinish = {
                            // Lưu trạng thái đã xem vào DataStore
                            CoroutineScope(Dispatchers.IO).launch {
                                limitsStore.saveOnboardingCompleted()
                            }
                            // Chuyển sang màn hình Giới thiệu (Home)
                            nav.navigate(Routes.Home) {
                                popUpTo(Routes.Onboarding) { inclusive = true }
                            }
                        }
                    )
                }

                // --- MÀN HÌNH GIỚI THIỆU (Introduce) ---
                composable(Routes.Home) {
                    introduceScreen(
                        onGoLogin = { nav.navigate(Routes.Login) }
                    )
                }

                // --- MÀN HÌNH LOGIN ---
                composable(Routes.Login) {
                    LoginScreen(
                        googleSignInClient = googleSignInClient,
                        onLogin = {
                            if (checkUsageAccessPermission(appContext)) {
                                nav.navigate(Routes.Main) {
                                    popUpTo(Routes.Home) { inclusive = true }
                                }
                            } else {
                                nav.navigate(Routes.Permission) {
                                    popUpTo(Routes.Home) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                // --- MÀN HÌNH CẤP QUYỀN ---
                composable(Routes.Permission) {
                    PermissionScreen(
                        onGoToSettings = {
                            try {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                activityContext.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        onCancel = {
                            nav.navigate(Routes.Main) {
                                popUpTo(Routes.Permission) { inclusive = true }
                            }
                        },
                        onPermissionGranted = {
                            nav.navigate(Routes.Main) {
                                popUpTo(Routes.Permission) { inclusive = true }
                            }
                        }
                    )
                }

                // --- MÀN HÌNH CHÍNH (MAIN) ---
                composable(Routes.Main) {
                    val currentHasPermission = checkUsageAccessPermission(appContext)
                    MainScreen(
                        mainNavController = nav,
                        hasPermission = currentHasPermission,
                        onLogout = {
                            authViewModel.logout()
                            nav.navigate(Routes.Home) {
                                popUpTo(Routes.Main) { inclusive = true }
                            }
                        },
                        promodoroViewModel = promodoroViewModel,
                        onNavigateToFocus = {
                            nav.navigate(Routes.FocusMode)
                        }
                    )
                }

                // --- MÀN HÌNH FOCUS MODE ---
                composable(Routes.FocusMode) {
                    FocusModeScreen(
                        viewModel = promodoroViewModel,
                        onExit = {
                            nav.popBackStack()
                        }
                    )
                }
            }
        }
    }
}