@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ducktrack.ui.login

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ducktrack.R
import com.example.ducktrack.ui.AuthViewModel
import com.example.ducktrack.ui.AppRoot.AuthViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.example.ducktrack.ui.theme.AppColors

// --- ĐỊNH NGHĨA FONT FAMILY ---
val JostFontFamily = FontFamily(
    Font(R.font.jost_extrabold, FontWeight.ExtraBold)
)
// ------------------------------------------

@Composable
fun LoginScreen(
    onLogin: () -> Unit = {},
    googleSignInClient: GoogleSignInClient
) {
    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(context.applicationContext)
    )
    val coroutineScope = rememberCoroutineScope()

    // 1. LOGIC ĐĂNG NHẬP GOOGLE
    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task: Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount> =
                GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken

                if (idToken != null) {
                    coroutineScope.launch {
                        viewModel.signInWithGoogleToken(idToken)
                        onLogin()
                    }
                } else {
                    Toast.makeText(context, "Lỗi: Không nhận được ID Token", Toast.LENGTH_SHORT).show()
                }

            } catch (e: ApiException) {
                Toast.makeText(context, "Đăng nhập Google thất bại: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else if (result.resultCode != CommonStatusCodes.CANCELED) {
            Toast.makeText(context, "Hủy đăng nhập", Toast.LENGTH_SHORT).show()
        }
    }

    val signInWithGoogle: () -> Unit = {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleAuthLauncher.launch(signInIntent)
        }
    }

    // 2. LOGIC ĐĂNG NHẬP GITHUB
    val signInWithGithub: () -> Unit = {
        val activity = context as? Activity
        if (activity != null) {
            viewModel.signInWithGithub(
                activity = activity,
                onSuccess = {
                    onLogin() // Chuyển màn hình khi thành công
                },
                onError = { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(context, "Lỗi thiết bị: Không tìm thấy Activity", Toast.LENGTH_SHORT).show()
        }
    }

    // --- GIAO DIỆN RESPONSIVE ---
    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.BackgroundWhite)
                .padding(inner),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // PHẦN 1: LOGO (Chiếm phần lớn màn hình - Tự động co giãn)
            Box(
                modifier = Modifier
                    .weight(1f) // Quan trọng: Chiếm hết không gian thừa
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_bubble_background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(0.9f), // Co lại xíu cho đẹp
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_duck_logo),
                    contentDescription = "DuckTrack Logo",
                    // Logo chiếm 50% chiều cao của Box chứa nó, giữ tỷ lệ 1:1
                    modifier = Modifier.fillMaxHeight(0.5f).aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
            }

            // PHẦN 2: TIÊU ĐỀ & NÚT BẤM (Luôn nằm gọn ở dưới)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp), // Cách đáy màn hình
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val textStyleWithShadow = TextStyle(
                    fontFamily = JostFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 40.sp,
                    color = AppColors.TextGreen,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.2f),
                        offset = androidx.compose.ui.geometry.Offset(4f, 4f),
                        blurRadius = 8f
                    )
                )

                Text(
                    text = "Đăng nhập",
                    style = textStyleWithShadow,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(32.dp))

                // Nút Google
                AuthButton(
                    text = "Đăng nhập bằng Google",
                    iconResId = R.drawable.ic_google,
                    backgroundColor = Color(0xFFF0F0F0),
                    contentColor = Color.Black,
                    onClick = signInWithGoogle
                )

                Spacer(Modifier.height(16.dp))

                // Nút GitHub
                AuthButton(
                    text = "Đăng nhập bằng GitHub",
                    iconResId = R.drawable.ic_github, // Đảm bảo file ảnh ic_github có trong drawable
                    backgroundColor = Color(0xFF24292E),
                    contentColor = Color.White,
                    onClick = signInWithGithub
                )
            }
        }
    }
}

// Component nút bấm dùng chung (Đã tối ưu chiều cao)
@Composable
fun AuthButton(
    text: String,
    iconResId: Int,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp), // Chiều cao vừa phải, không quá lớn
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(28.dp) // Icon nhỏ gọn hơn chút
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            // Spacer ảo để icon bên trái không đẩy chữ lệch
            Spacer(Modifier.width(28.dp))
        }
    }
}