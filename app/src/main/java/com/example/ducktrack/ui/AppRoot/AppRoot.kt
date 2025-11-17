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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ducktrack.ui.AuthViewModel
import com.example.ducktrack.ui.introducePage.introduceScreen
import com.example.ducktrack.ui.login.LoginScreen
import com.example.ducktrack.ui.main.MainScreen
import com.example.ducktrack.ui.permission.PermissionScreen
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ducktrack.ui.theme.DuckTrackTheme
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.example.ducktrack.utils.hasUsageAccess
import androidx.compose.runtime.remember // <-- Thêm import này

// (Factory và hàm checkUsageAccessPermission giữ nguyên)
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

    // --- SỬA LẠI HOÀN TOÀN LOGIC KHỞI ĐỘNG ---

    // 1. Lấy trạng thái đăng nhập TỪ VIEWMODEL (ViewModel đọc từ SharedPreferences)
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    // 2. Kiểm tra quyền truy cập
    val hasPermission = checkUsageAccessPermission(appContext)

    // 3. Quyết định màn hình khởi động
    val startDestination = remember(isAuthenticated, hasPermission) {
        if (isAuthenticated) {
            // NẾU ĐÃ ĐĂNG NHẬP:
            if (hasPermission) {
                Routes.Main // -> Vào thẳng Dashboard
            } else {
                Routes.Permission // -> Vào màn hình xin quyền
            }
        } else {
            // NẾU CHƯA ĐĂNG NHẬP:
            Routes.Home // -> Vào trang Mở đầu (introducePage)
        }
    }
    // ------------------------------------------

    DuckTrackTheme {
        Scaffold { innerPadding ->
            NavHost(
                navController = nav,
                startDestination = startDestination, // <-- SỬ DỤNG BIẾN ĐỘNG
                modifier = Modifier.padding(innerPadding)
            ) {

                // 1. Routes.Home (Trang mở đầu - introducePage)
                composable(Routes.Home) {
                    introduceScreen(
                        onGoLogin = { nav.navigate(Routes.Login) }
                    )
                }

                // 2. Routes.Login (Màn hình đăng nhập - login.kt)
                composable(Routes.Login) {
                    LoginScreen(
                        googleSignInClient = googleSignInClient,
                        onLogin = {
                            // 3. Sau khi Đăng nhập thành công -> kiểm tra quyền
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

                // 4. Routes.Permission (Màn hình xin cấp quyền - PermissionScreen)
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
                        // 5. Sau khi Cấp quyền HOẶC Hủy -> luôn về Routes.Main
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

                // 6. Routes.Main (Màn hình chính chứa DashboardScreen)
                composable(Routes.Main) {
                    val currentHasPermission = checkUsageAccessPermission(appContext)

                    MainScreen(
                        mainNavController = nav,
                        hasPermission = currentHasPermission,
                        onLogout = {
                            authViewModel.logout()
                            nav.navigate(Routes.Home) { // Đăng xuất -> Quay về Trang mở đầu
                                popUpTo(Routes.Main) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}