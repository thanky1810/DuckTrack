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
    // ĐÃ XÓA onGoHome: () -> Unit = {},
    onLogin: () -> Unit = {},
    googleSignInClient: GoogleSignInClient
) {
    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(context.applicationContext)
    )
    val coroutineScope = rememberCoroutineScope()

    // (Logic Google Sign-In giữ nguyên)
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

    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.BackgroundWhite)
                .padding(inner)
                .padding(top = 40.dp, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // (Logo, Tiêu đề, và các Nút giữ nguyên)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_bubble_background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().scale(1.1f),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_duck_logo),
                    contentDescription = "DuckTrack Logo",
                    modifier = Modifier.size(250.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(24.dp))

            val textStyleWithShadow = TextStyle(
                fontFamily = JostFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 40.sp,
                color = AppColors.TextGreen,
                textAlign = TextAlign.Center,
                lineHeight = 48.sp,
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

            Spacer(Modifier.height(40.dp))

            AuthButton(
                text = "Đăng nhập bằng Google",
                iconResId = R.drawable.ic_google,
                backgroundColor = Color(0xFFF0F0F0),
                contentColor = Color.Black,
                onClick = signInWithGoogle
            )

            Spacer(Modifier.height(24.dp))

            AuthButton(
                text = "Đăng nhập bằng Facebook",
                iconResId = R.drawable.ic_facebook,
                backgroundColor = Color(0xFF3B5998),
                contentColor = Color.White,
                onClick = { Toast.makeText(context, "Chức năng đang phát triển", Toast.LENGTH_SHORT).show() }
            )
        }
    }
}

// (Hàm AuthButton giữ nguyên)
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
            .height(80.dp)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(32.dp))
        }
    }
}