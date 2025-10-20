@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ducktrack.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
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
import com.example.ducktrack.ui.components.fields.PillTextField


@Composable
fun LoginScreen(
    onGoHome: () -> Unit = {},
    onLogin: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onGoSignUp: () -> Unit = {}
) {
    var input by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPwd by rememberSaveable { mutableStateOf(false) }

    val green = Color(0xFF3F8D53)     // màu nút
    val fieldGreen = Color(0xFF6FB36C) // nền ô nhập

    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Logo + nền (giữ nguyên của bạn)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "DuckTrack Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(250.dp)
                        .offset(y = (-55).dp),
                    contentScale = ContentScale.Fit
                )
            }

            Text(
                text = "Đăng nhập",
                fontSize = 40.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .offset(y = (-40).dp),
                style = LocalTextStyle.current.copy(
                    shadow = Shadow(color = Color(0x22000000), blurRadius = 6f)
                )
            )

            Spacer(Modifier.height(24.dp))

            // Ô "Tên người dùng" kiểu viên thuốc
            PillTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = "Tên người dùng",
                leading = { Icon(Icons.Default.Person, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                containerColor = fieldGreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-40).dp)
            )

            Spacer(Modifier.height(12.dp))

            // Ô "Mật khẩu" kiểu viên thuốc + icon ẩn/hiện
            PillTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Mật khẩu",
                leading = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailing = {
                    IconButton(onClick = { showPwd = !showPwd }) {
                        Icon(
                            if (showPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPwd) "Ẩn mật khẩu" else "Hiện mật khẩu"
                        )
                    }
                },
                visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                containerColor = fieldGreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-40).dp)
            )

            Spacer(Modifier.height(20.dp))

            // Nút "Đăng nhập" đồng bộ style
            Button(
                onClick = onLogin,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = green,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .offset(y = (-40).dp)
            ) {
                Text("Đăng nhập", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .offset(y = (-40).dp),
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

