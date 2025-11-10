package com.example.ducktrack.ui.AppRoot

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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

// Hàm kiểm tra quyền
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
    // val isAuthenticated by authViewModel.isAuthenticated.collectAsState() // Vẫn cần cho Logout

    // --- SỬA LẠI HOÀN TOÀN LOGIC KHỞI ĐỘNG ---
    // Ứng dụng LUÔN BẮT ĐẦU TẠI TRANG MỞ ĐẦU (introducePage)
    // Chúng ta không kiểm tra isAuthenticated ở đây nữa.
    val startDestination = Routes.Home
    // ------------------------------------------

    DuckTrackTheme {
        Scaffold { innerPadding ->
            NavHost(
                navController = nav,
                startDestination = startDestination, // Luôn là Routes.Home
                modifier = Modifier.padding(innerPadding)
            ) {

                // 1. Routes.Home (Trang mở đầu - introducePage)
                composable(Routes.Home) {
                    introduceScreen(
                        // Khi nhấn "Đăng nhập" -> đi tới Routes.Login
                        onGoLogin = { nav.navigate(Routes.Login) }
                    )
                }

                // 2. Routes.Login (Màn hình đăng nhập - login.kt)
                composable(Routes.Login) {
                    LoginScreen(
                        googleSignInClient = googleSignInClient,
                        onLogin = {
                            // 3. Sau khi Đăng nhập thành công -> đi tới Routes.Permission
                            nav.navigate(Routes.Permission) {
                                // Xóa stack về Home để người dùng không quay lại được
                                popUpTo(Routes.Home) { inclusive = true }
                            }
                        },
                        //onGoHome = { nav.popBackStack() } // Nút quay lại từ Login về Home
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
                        // 5. Sau khi Cấp quyền HOẶC Hủy -> luôn về Routes.Main (DashboardScreen)
                        onCancel = {
                            nav.navigate(Routes.Main) {
                                popUpTo(Routes.Permission) { inclusive = true }
                            }
                        },
                        onPermissionGranted = { // Khi quay lại app và thấy đã có quyền
                            nav.navigate(Routes.Main) {
                                popUpTo(Routes.Permission) { inclusive = true }
                            }
                        }
                    )
                }

                // 6. Routes.Main (Màn hình chính chứa DashboardScreen)
                composable(Routes.Main) {
                    // MainScreen sẽ tự kiểm tra quyền và hiển thị 1 trong 2 UI
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