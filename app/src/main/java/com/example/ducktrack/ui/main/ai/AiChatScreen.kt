package com.example.ducktrack.ui.main.ai

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ducktrack.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: AiChatViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as android.app.Application)
    )

    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val duckName by viewModel.duckName.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Tự động cuộn xuống cuối khi mở màn hình hoặc có tin nhắn mới
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // Xử lý bàn phím che
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "$duckName 🦆", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            text = "Trợ lý ảo & Giáo viên khó tính",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.analyzeMyHabits() }) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "Phân tích",
                            tint = AppColors.ButtonGreen
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(Color.White),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Hỏi thầy Vịt...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.ButtonGreen,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier.background(AppColors.ButtonGreen, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg, viewModel)
            }
            if (isLoading) {
                item {
                    Text(
                        "$duckName đang suy nghĩ...",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, viewModel: AiChatViewModel) {
    val isUser = message.isUser
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Format thời gian: 14:30
    val timeString = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Surface(
                color = if (isUser) AppColors.ButtonGreen else Color.White,
                shape = RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                shadowElevation = 2.dp,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = formatAiText(message.text), // <-- Dùng hàm mình vừa viết
                        color = if (isUser) Color.White else Color.Black,
                        fontSize = 15.sp
                    )

                    // Hiển thị thời gian
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeString,
                        color = if (isUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            // --- HÀNG NÚT CHỨC NĂNG (CHỈ HIỆN CHO TIN NHẮN CỦA BOT) ---
            if (!isUser) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Nút Nghe (Loa)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Nghe",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { viewModel.speakMessage(message.text) }
                    )

                    // Nút Copy
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Sao chép",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                clipboardManager.setText(AnnotatedString(message.text))
                                Toast.makeText(context, "Đã sao chép!", Toast.LENGTH_SHORT).show()
                            }
                    )
                }
            }
        }
    }
}

fun formatAiText(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        // Regex này sẽ tìm tất cả các đoạn chữ bị kẹp giữa **...** hoặc __...__
        val pattern = Regex("\\*\\*(.*?)\\*\\*|__(.*?)__")
        val matches = pattern.findAll(text)

        for (match in matches) {
            // 1. Nối phần chữ bình thường (nằm trước ký hiệu) vào
            append(text.substring(currentIndex, match.range.first))

            // 2. Xử lý phần có ký hiệu
            if (match.value.startsWith("**")) {
                // Nhóm 1: Là Thời gian -> In đậm
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(match.groupValues[1])
                }
            } else if (match.value.startsWith("__")) {
                // Nhóm 2: Là Tên App -> In nghiêng + Gạch chân + Màu xanh
                withStyle(
                    style = SpanStyle(
                        fontStyle = FontStyle.Italic,
                        textDecoration = TextDecoration.Underline,
                        color = Color(0xFF2E7D32), // Màu xanh AppColors.ButtonGreen
                        fontWeight = FontWeight.Bold // Cho đậm lên chút nhìn cho rõ
                    )
                ) {
                    append(match.groupValues[2])
                }
            }
            // Cập nhật lại vị trí con trỏ
            currentIndex = match.range.last + 1
        }

        // 3. Nối nốt phần chữ bình thường còn sót lại ở cuối câu
        append(text.substring(currentIndex))
    }
}

