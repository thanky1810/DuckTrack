@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ducktrack.ui.forgotpassword

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ducktrack.R
import com.example.ducktrack.ui.components.fields.PillTextField
import com.example.ducktrack.ui.introducePage.AppColors

@Composable
fun ForgotPasswordScreen(
    onGoBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    val green = AppColors.Green
    val fieldGreen = green.copy(alpha = 0.5f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onGoBack) {
                        Icon(
                            // Sửa từ Icons.Default thành Icons.Filled cho nhất quán
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_duck_logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(16.dp))

            Text(
                "Quên mật khẩu",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = green
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Nhập email của bạn để nhận liên kết đặt lại mật khẩu.",
                textAlign = TextAlign.Center,
                color = Color.Gray
            )

            Spacer(Modifier.height(32.dp))

            PillTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email",
                // Sửa từ Icons.Default thành Icons.Filled cho nhất quán
                leading = { Icon(Icons.Filled.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                containerColor = fieldGreen,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { /* TODO: Handle password reset logic */ onGoBack() },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = green,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Gửi liên kết", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

