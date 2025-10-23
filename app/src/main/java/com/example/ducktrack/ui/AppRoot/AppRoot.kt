package com.example.ducktrack.ui.AppRoot

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
<<<<<<< HEAD
import com.example.ducktrack.ui.AuthViewModel
import com.example.ducktrack.ui.forgotpassword.ForgotPasswordScreen
=======
import androidx.lifecycle.viewmodel.compose.viewModel

>>>>>>> 7333551 (WIP: keep my local changes before syncing)
import com.example.ducktrack.ui.introducePage.introduceScreen
import com.example.ducktrack.ui.login.LoginScreen
import com.example.ducktrack.ui.main.MainScreen
import com.example.ducktrack.ui.permission.PermissionScreen
import com.example.ducktrack.ui.signup.SignUpScreen
<<<<<<< HEAD
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
=======
import com.example.ducktrack.ui.permission.PermissionScreen
import com.example.ducktrack.ui.home.HomeScreen
import com.example.ducktrack.ui.home.HomeViewModel
>>>>>>> 7333551 (WIP: keep my local changes before syncing)

// Factory để truyền Context vào ViewModel
class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Hàm kiểm tra quyền truy cập (ngoài Composable)
private fun hasUsageAccessPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppRoot() {
    val nav = rememberNavController()

    val appContext = LocalContext.current.applicationContext

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(appContext)
    )

    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    // --- LOGIC CẬP NHẬT: startDestination ---
    // Kiểm tra quyền ngay khi khởi động
    val hasPermission = hasUsageAccessPermission(appContext)

    // Quyết định màn hình bắt đầu
    val startDestination = if (isAuthenticated) {
        if (hasPermission) {
            // Đã đăng nhập VÀ có quyền -> Vào thẳng Main
            Routes.Main
        } else {
            // Đã đăng nhập NHƯNG chưa có quyền -> Vào màn hình Permission
            Routes.Permission
        }
    } else {
        // Chưa đăng nhập -> Về Home
        Routes.Home
    }
    // --- KẾT THÚC CẬP NHẬT ---

    Scaffold { inner ->
        NavHost(
            navController = nav,
<<<<<<< HEAD
            startDestination = startDestination, // Sử dụng biến động
            modifier = Modifier.padding(inner)
        ){
            composable(Routes.Home) {
                introduceScreen(
                    onForgotPassword = { nav.navigate(Routes.ForgotPassword) },
                    onCreateAccount = { nav.navigate(Routes.SignUp) },
                    onGoLogin = { nav.navigate(Routes.Login) }
=======
            startDestination = Routes.Introduce,
            modifier = Modifier.padding(inner)
        ) {
            composable(Routes.Introduce) {
                introduceScreen(
                    onGoLogin = { nav.navigateSingleTop(Routes.Login) }
>>>>>>> 7333551 (WIP: keep my local changes before syncing)
                )
            }

            composable(Routes.Login) {
<<<<<<< HEAD
                LoginScreen(
                    viewModel = authViewModel,
                    // Luồng: Sau khi đăng nhập, PHẢI đi tới trang Permission
                    onLogin = {
                        nav.navigate(Routes.Permission) {
                            popUpTo(Routes.Login) { inclusive = true }
                        }
                    },
                    onGoSignUp = {
                        nav.navigate(Routes.SignUp) {
                            popUpTo(Routes.Login) { inclusive = true }
                        }
                    },
                    onForgotPassword = { nav.navigate(Routes.ForgotPassword) }
=======
                // BẤM LOGIN → đi thẳng đến màn xin quyền
                LoginScreen(
                    onLogin = { nav.navigateSingleTop(Routes.Permission) },
                    onGoHome = { nav.navigateSingleTop(Routes.Home) }, // (không dùng, nhưng giữ cho tiện)
                    onGoSignUp = { nav.navigateSingleTop(Routes.SignUp) }
>>>>>>> 7333551 (WIP: keep my local changes before syncing)
                )
            }

            composable(Routes.SignUp) {
                SignUpScreen(
<<<<<<< HEAD
                    viewModel = authViewModel,
                    onGoLogin = {
                        nav.navigate(Routes.Login) {
                            popUpTo(Routes.SignUp) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.ForgotPassword) {
                ForgotPasswordScreen(
                    onGoBack = { nav.popBackStack() }
                )
            }

            composable(Routes.Permission) {
                val activityContext = LocalContext.current

                PermissionScreen(
                    onGoToSettings = {
                        try {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            activityContext.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    // Khi nhấn Hủy, đi tới Main (sẽ hiển thị UI yêu cầu)
                    onCancel = {
                        nav.navigate(Routes.Main) {
                            popUpTo(Routes.Permission) { inclusive = true }
                        }
                    },
                    // Khi cấp quyền xong, cũng đi tới Main
                    onPermissionGranted = {
                        nav.navigate(Routes.Main) {
                            popUpTo(Routes.Permission) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.Main) {
                // MainScreen sẽ tự kiểm tra quyền khi được điều hướng tới
                val currentHasPermission = hasUsageAccessPermission(appContext)

                MainScreen(
                    mainNavController = nav,
                    hasPermission = currentHasPermission, // Truyền trạng thái quyền
                    onLogout = {
                        authViewModel.logout()
                        nav.navigate(Routes.Home) {
                            popUpTo(Routes.Main) { inclusive = true }
                        }
                    }
=======
                    onGoLogin = { nav.navigateSingleTop(Routes.Login) }
>>>>>>> 7333551 (WIP: keep my local changes before syncing)
                )
            }

            // MÀN XIN QUYỀN → khi đã có quyền hoặc bấm nút xác nhận → vào Home
            composable(Routes.Permission) {
                PermissionScreen(
                    onGranted = {
                        nav.navigateSingleTop(Routes.Home)
                    }
                )
            }

            // HOME
            composable(Routes.Home) {
                val vm: HomeViewModel = viewModel()
                HomeScreen(vm)
            }
        }
    }
<<<<<<< HEAD
}
=======
}

// Giữ helper điều hướng singleTop như bạn đã có
private fun androidx.navigation.NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(graph.startDestinationId) { saveState = true }
    }
}
>>>>>>> 7333551 (WIP: keep my local changes before syncing)
