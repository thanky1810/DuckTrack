@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ducktrack.ui.login

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ducktrack.R
import com.example.ducktrack.ui.AuthViewModel
import com.example.ducktrack.ui.components.fields.PillTextField
import com.example.ducktrack.ui.introducePage.AppColors

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onGoHome: () -> Unit = {},
    onGoSignUp: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onLogin: () -> Unit = {}
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    val green = AppColors.Green
    val fieldGreen = green.copy(alpha = 0.5f)
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onGoHome) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sử dụng cấu trúc Box từ introducePage.kt để đảm bảo căn giữa và bố cục nhất quán
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(150.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Đăng nhập",
                textAlign = TextAlign.Center,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineLarge.copy(
                    shadow = Shadow(
                        color = Color(0x40000000),
                        blurRadius = 8f
                    )
                )
            )

            Spacer(Modifier.height(32.dp))

            PillTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = "Tên đăng nhập",
                leading = { Icon(Icons.Default.Person, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                containerColor = fieldGreen,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))
            PillTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Mật khẩu",
                leading = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailing = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        val icon = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        Icon(icon, contentDescription = "Toggle password visibility")
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                containerColor = fieldGreen,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

<<<<<<< HEAD
=======

>>>>>>> 7333551 (WIP: keep my local changes before syncing)
            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                    } else {
                        if (viewModel.login(username, password)) {
                            onLogin()
                        } else {
                            Toast.makeText(context, "Tên đăng nhập hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = green,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Đăng nhập", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onForgotPassword) {
                    Text("Quên mật khẩu?", color = Color(0xFF3A3A3A), fontSize = 20.sp)
                }
                TextButton(onClick = onGoSignUp) {
                    Text("Tạo tài khoản", color = Color(0xFF2E8B57), fontSize = 20.sp)
                }
            }
        }
    }
}

