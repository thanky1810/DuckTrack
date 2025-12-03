package com.example.ducktrack.ui.AppRoot

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
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
import com.example.ducktrack.ui.AuthViewModel
import com.example.ducktrack.ui.introducePage.introduceScreen
import com.example.ducktrack.ui.login.LoginScreen
import com.example.ducktrack.ui.main.MainScreen
import com.example.ducktrack.ui.main.ViewModelFactory
import com.example.ducktrack.ui.main.promodoro.FocusModeScreen
import com.example.ducktrack.ui.main.promodoro.PromodoroViewModel
import com.example.ducktrack.ui.permission.PermissionScreen
import com.example.ducktrack.ui.theme.DuckTrackTheme
import com.example.ducktrack.utils.hasUsageAccess
import com.google.android.gms.auth.api.signin.GoogleSignInClient

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

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(appContext)
    )

    // --- KHỞI TẠO SHARED POMODORO VIEWMODEL TẠI ĐÂY ---
    // Để đảm bảo Timer không bị reset khi chuyển màn hình
    val promodoroViewModel: PromodoroViewModel = viewModel(
        factory = ViewModelFactory(appContext as MyApplication)
    )

    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val hasPermission = checkUsageAccessPermission(appContext)

    val startDestination = remember(isAuthenticated, hasPermission) {
        if (isAuthenticated) {
            if (hasPermission) Routes.Main else Routes.Permission
        } else {
            Routes.Home
        }
    }

    DuckTrackTheme {
        Scaffold { innerPadding ->
            NavHost(
                navController = nav,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Routes.Home) {
                    introduceScreen(
                        onGoLogin = { nav.navigate(Routes.Login) }
                    )
                }

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

                // ROUTE MAIN: Truyền Shared ViewModel xuống
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
                        promodoroViewModel = promodoroViewModel, // <-- Truyền vào
                        onNavigateToFocus = { // <-- Callback mở màn hình Focus
                            nav.navigate(Routes.FocusMode)
                        }
                    )
                }

                // ROUTE FOCUS MODE: Màn hình Full Screen mới
                composable(Routes.FocusMode) {
                    FocusModeScreen(
                        viewModel = promodoroViewModel, // <-- Dùng chung ViewModel để đồng bộ giờ
                        onExit = {
                            nav.popBackStack() // Quay về Main
                        }
                    )
                }
            }
        }
    }
}