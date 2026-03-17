@file:Suppress("DEPRECATION")
@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ducktrack.ui.login

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ducktrack.ui.approot.AuthViewModelFactory
import com.example.ducktrack.ui.theme.AppColors
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

// --- ĐỊNH NGHĨA FONT FAMILY (Đã đổi sang Montserrat) ---
val MainFontFamily = FontFamily(
    Font(R.font.montserrat_extrabold, FontWeight.ExtraBold)
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

    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken
                if (idToken != null) {
                    coroutineScope.launch {
                        viewModel.signInWithGoogleToken(idToken)
                        onLogin()
                    }
                } else {
                    Toast.makeText(context, "Lỗi Token", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val signInWithGoogle: () -> Unit = {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleAuthLauncher.launch(signInIntent)
        }
    }

    val signInWithGithub: () -> Unit = {
        val activity = context as? Activity
        if (activity != null) {
            viewModel.signInWithGithub(
                activity = activity,
                onSuccess = { onLogin() },
                onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
            )
        }
    }

    // --- GIAO DIỆN ---
    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(inner),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // PHẦN 1: LOGO
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_bubble_background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(0.9f),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_duck_logo),
                    contentDescription = "DuckTrack Logo",
                    modifier = Modifier
                        .fillMaxHeight(0.5f)
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
            }

            // PHẦN 2: TIÊU ĐỀ & NÚT BẤM
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val textStyleWithShadow = TextStyle(
                    fontFamily = MainFontFamily,
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

                AuthButton(
                    text = "Đăng nhập bằng Google",
                    iconResId = R.drawable.ic_google,
                    backgroundColor = AppColors.ButtonGreen,
                    contentColor = Color.White,
                    onClick = signInWithGoogle
                )

                Spacer(Modifier.height(16.dp))

                AuthButton(
                    text = "Đăng nhập bằng GitHub",
                    iconResId = R.drawable.ic_github,
                    backgroundColor = AppColors.ButtonGreen,
                    contentColor = Color.White,
                    onClick = signInWithGithub
                )
            }
        }
    }
}

// Button giữ nguyên
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
            .height(70.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text,
                // Dùng font Montserrat cho nút bấm luôn cho đẹp
                fontFamily = MainFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold, // Montserrat ExtraBold hoặc Bold đều đẹp
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(40.dp))
        }
    }
}