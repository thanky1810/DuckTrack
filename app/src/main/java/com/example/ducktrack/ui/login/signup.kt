@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ducktrack.ui.signup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
fun SignUpScreen(
    onSignUp: () -> Unit = {},
    onGoLogin: () -> Unit = {}
) {
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPwd by rememberSaveable { mutableStateOf(false) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }

    val green = Color(0xFF3F8D53)
    val fieldGreen = Color(0xFF6FB36C)


    Scaffold(
        contentWindowInsets = WindowInsets(0.dp) // tuỳ chọn: tự kiểm soát insets
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // LOGO 230dp, bỏ padding bottom lớn
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(230.dp)
                    .padding(top = 8.dp, bottom = 8.dp)
                    .offset(y = (-10).dp)
            )

            // ==== NGUYÊN KHỐI FORM ĐƯỢC NÂNG LÊN ====
            Column(modifier = Modifier.offset(y = (-45).dp)) {
                // Tiêu đề
                Text(
                    "Đăng ký",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Ô: Tên người dùng
                PillTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = "Tên người dùng",
                    leading = { Icon(Icons.Default.Person, contentDescription = null) },
                    containerColor = fieldGreen,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                // Ô: Email
                PillTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Email",
                    leading = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    containerColor = fieldGreen,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                // Ô: Mật khẩu
                PillTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Mật khẩu",
                    leading = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailing = {
                        IconButton(onClick = { showPwd = !showPwd }) {
                            Icon(
                                imageVector = if (showPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPwd) "Ẩn mật khẩu" else "Hiện mật khẩu"
                            )
                        }
                    },
                    visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    containerColor = fieldGreen,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                // Ô: Xác thực lại mật khẩu
                PillTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    placeholder = "Xác thực lại mật khẩu",
                    leading = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailing = {
                        IconButton(onClick = { showConfirm = !showConfirm }) {
                            Icon(
                                imageVector = if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showConfirm) "Ẩn mật khẩu" else "Hiện mật khẩu"
                            )
                        }
                    },
                    visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    containerColor = fieldGreen,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(18.dp))

                // Nút Đăng ký
                Button(
                    onClick = onSignUp,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = green,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Đăng ký", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))

                // Dòng “Đã có tài khoản?”
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Bạn đã có tài khoản? ", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onGoLogin, contentPadding = PaddingValues(0.dp)) {
                        Text("Đăng nhập", color = Color(0xFF2E8B57), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

}

