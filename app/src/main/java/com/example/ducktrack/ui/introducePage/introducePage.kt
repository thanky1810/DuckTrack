package com.example.ducktrack.ui.introducePage

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ducktrack.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun introduceScreen(
    onForgotPassword: () -> Unit = {},
    onCreateAccount: () -> Unit = {},
    onGoLogin: () -> Unit = {}
) {
    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(24.dp))

            // Khối hình: nền bong bóng + logo vịt chồng lên
            Box(
                modifier = Modifier
                    .padding(top =40.dp)
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
                text = "Plant your Forest\nGrow your Focus",
                color = Color(0xFF294E2D), // xanh đậm
                fontSize = 45.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
                    .offset(y = (-40).dp),
                style = LocalTextStyle.current.copy(
                    shadow = Shadow( // nhẹ để chữ nổi hơn
                        color = Color(0x22000000),
                        blurRadius = 6f
                    )
                )
            )

            Spacer(Modifier.height(24.dp))


            Button(

                onClick = onGoLogin,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3F8D53), // xanh nút
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .fillMaxWidth()
                    .height(65.dp)
            ) {
                Text("Bắt đầu", fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            // Hàng link phụ
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onForgotPassword) {
                    Text("Quên mật khẩu?", color = Color(0xFF3A3A3A), fontSize = 20.sp)
                }
                TextButton(onClick = onCreateAccount) {
                    Text("Tạo tài khoản", color = Color(0xFF2E8B57), fontSize = 20.sp)
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}
