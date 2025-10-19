package com.example.ducktrack.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onGoHome: () -> Unit = {},
    onLogin: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onCreateAccount: () -> Unit = {},
) {
    var input by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPwd by rememberSaveable { mutableStateOf(false) }

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

            // Khối hình: nền bong bóng + logo vịt
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img), // nền bong bóng
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(),
                    contentScale = ContentScale.Fit
                )

                Image(
                    painter = painterResource(id = R.drawable.logo),  // logo vịt
                    contentDescription = "DuckTrack Logo",
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .size(250.dp)
                        .offset(y = (-55).dp),
                    contentScale = ContentScale.Fit
                )
            }

            // Headline
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
                    shadow = Shadow(
                        color = Color(0x22000000),
                        blurRadius = 6f
                    )
                )
            )

            Spacer(Modifier.height(24.dp))


            TextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Email hoặc tên đăng nhập") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
                    .offset(y = (-40).dp)
            )

            Spacer(Modifier.height(12.dp))


            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật khẩu") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()  .offset(y = (-40).dp),
                visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = { showPwd = !showPwd }) {
                        Icon(
                            imageVector = if (showPwd) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPwd) "Ẩn mật khẩu" else "Hiện mật khẩu"
                        )
                    }
                }
            )

            Spacer(Modifier.height(20.dp))

            // Nút Đăng nhập
            Button(
                onClick = onLogin,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3F8D53),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)  .offset(y = (-40).dp)
            ) {
                Text("Bắt đầu", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            // Hàng link phụ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)  .offset(y = (-40).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onForgotPassword) {
                    Text("Quên mật khẩu?", color = Color(0xFF3A3A3A), fontSize = 16.sp)
                }
                TextButton(onClick = onCreateAccount) {
                    Text("Tạo tài khoản", color = Color(0xFF2E8B57), fontSize = 16.sp)
                }
            }
        }
    }
}
