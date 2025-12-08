// FILE: ui/AppRoot/AppRoot.kt
package com.example.ducktrack.ui.AppRoot

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.example.ducktrack.ui.main.ai.AiChatScreen
import com.example.ducktrack.ui.main.pomodoro.FocusModeScreen
import com.example.ducktrack.ui.main.pomodoro.PomodoroViewModel
import com.example.ducktrack.ui.main.settings.AboutUsScreen // <--- IMPORT MỚI
import com.example.ducktrack.ui.main.settings.AchievementsScreen
import com.example.ducktrack.ui.main.settings.ExportHistoryScreen
import com.example.ducktrack.ui.main.settings.UserProfileScreen
import com.example.ducktrack.ui.onboarding.OnboardingScreen
import com.example.ducktrack.ui.permission.PermissionScreen
import com.example.ducktrack.ui.splash.SplashScreen
import com.example.ducktrack.ui.theme.DuckTrackTheme
import com.example.ducktrack.utils.hasUsageAccess
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.ducktrack.ui.main.settings.StatisticsScreen

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

    val limitsStore = remember { LimitsStore(appContext) }

    val isOnboardingCompleted by limitsStore.onboardingCompleted.collectAsState(initial = null)

    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(appContext))
    val pomodoroViewModel: PomodoroViewModel = viewModel(factory = ViewModelFactory(appContext as MyApplication))

    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val hasPermission = checkUsageAccessPermission(appContext)

    DuckTrackTheme {
        Scaffold { innerPadding ->
            NavHost(
                navController = nav,
                startDestination = Routes.Splash,
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            ) {
                // SPLASH
                composable(Routes.Splash) {
                    SplashScreen(
                        isDataReady = (isOnboardingCompleted != null),
                        onSplashFinished = {
                            val nextRoute = if (isOnboardingCompleted == false) Routes.Onboarding
                            else if (isAuthenticated) (if (hasPermission) Routes.Main else Routes.Permission)
                            else Routes.Home
                            nav.navigate(nextRoute) { popUpTo(Routes.Splash) { inclusive = true } }
                        }
                    )
                }

                // ONBOARDING
                composable(Routes.Onboarding) {
                    OnboardingScreen(onFinish = {
                        CoroutineScope(Dispatchers.IO).launch { limitsStore.saveOnboardingCompleted() }
                        nav.navigate(Routes.Home) { popUpTo(Routes.Onboarding) { inclusive = true } }
                    })
                }

                // HOME / INTRO
                composable(Routes.Home) { introduceScreen(onGoLogin = { nav.navigate(Routes.Login) }) }

                // LOGIN
                composable(Routes.Login) {
                    LoginScreen(googleSignInClient = googleSignInClient, onLogin = {
                        if (checkUsageAccessPermission(appContext)) nav.navigate(Routes.Main) { popUpTo(Routes.Home) { inclusive = true } }
                        else nav.navigate(Routes.Permission) { popUpTo(Routes.Home) { inclusive = true } }
                    })
                }

                // PERMISSION
                composable(Routes.Permission) {
                    PermissionScreen(
                        onGoToSettings = { try { activityContext.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) {} },
                        onCancel = { nav.navigate(Routes.Main) { popUpTo(Routes.Permission) { inclusive = true } } },
                        onPermissionGranted = { nav.navigate(Routes.Main) { popUpTo(Routes.Permission) { inclusive = true } } }
                    )
                }

                // MAIN
                composable(Routes.Main) {
                    val currentHasPermission = checkUsageAccessPermission(appContext)
                    MainScreen(
                        mainNavController = nav,
                        hasPermission = currentHasPermission,
                        onLogout = {
                            authViewModel.logout()
                            nav.navigate(Routes.Home) { popUpTo(Routes.Main) { inclusive = true } }
                        },
                        pomodoroViewModel = pomodoroViewModel,
                        onNavigateToFocus = { nav.navigate(Routes.FocusMode) },
                        onNavigateToExportHistory = { nav.navigate(Routes.ExportHistory) },
                        onNavigateToAbout = { nav.navigate(Routes.AboutUs) },
                        onNavigateToAchievements = { nav.navigate(Routes.Achievements) },
                        onNavigateToStatistics = { nav.navigate(Routes.Statistics) },
                        // --- MỚI: Truyền xuống MainScreen ---
                        onNavigateToAiChat = { nav.navigate(Routes.AiChat) }
                    )
                }

                // SUB-SCREENS
                composable(Routes.FocusMode) { FocusModeScreen(viewModel = pomodoroViewModel, onExit = { nav.popBackStack() }) }
                composable(Routes.UserProfile) { UserProfileScreen(onBack = { nav.popBackStack() }) }
                composable(Routes.ExportHistory) { ExportHistoryScreen(onBack = { nav.popBackStack() }) }
                composable(Routes.AboutUs) { AboutUsScreen(onBack = { nav.popBackStack() }) }
                composable(Routes.Achievements) { AchievementsScreen(onBack = { nav.popBackStack() }) }
                composable(Routes.Statistics) { StatisticsScreen(onBack = { nav.popBackStack() }) }

                // --- MỚI: Màn hình AI Chat ---
                composable(Routes.AiChat) {
                    AiChatScreen(onBack = { nav.popBackStack() })
                }
            }
        }
    }
}