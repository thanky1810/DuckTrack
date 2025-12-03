// FILE: UserProfileScreen.kt
package com.example.ducktrack.ui.main.settings

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ducktrack.R
import com.example.ducktrack.ui.AppRoot.AuthViewModelFactory
import com.example.ducktrack.ui.AuthViewModel
import com.example.ducktrack.ui.UserInfo
import com.example.ducktrack.ui.main.garden.SeedType
import com.example.ducktrack.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onBack: () -> Unit,
    authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(LocalContext.current.applicationContext)
    )
) {
    val context = LocalContext.current
    var userInfo by remember { mutableStateOf<UserInfo?>(null) }

    // Dialog States
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDuckNameDialog by remember { mutableStateOf(false) }

    // Loading State
    val isUploading by authViewModel.isUploading.collectAsState()

    // --- 1. LOAD DATA ---
    LaunchedEffect(Unit) {
        authViewModel.loadUserInfo { info -> userInfo = info }
    }

    // --- 2. BỘ CHỌN ẢNH (AVATAR) ---
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            authViewModel.updateAvatar(
                uri = uri,
                onSuccess = {
                    Toast.makeText(context, "Cập nhật Avatar thành công!", Toast.LENGTH_SHORT).show()
                    authViewModel.loadUserInfo { info -> userInfo = info }
                },
                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hồ sơ cá nhân", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = AppColors.TextGreen
                )
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER: AVATAR & TÊN ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.TextGreen.copy(alpha = 0.05f))
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(16.dp))

                    // AVATAR
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(110.dp)
                            .clickable {
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(color = AppColors.TextGreen)
                        } else {
                            val base64Str = userInfo?.photoBase64
                            val bitmap = remember(base64Str) {
                                try {
                                    val b = Base64.decode(base64Str, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(b, 0, b.size).asImageBitmap()
                                } catch (e: Exception) { null }
                            }

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap, contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape).border(3.dp, AppColors.TextGreen, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.duck_waiting), contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape).border(3.dp, AppColors.TextGreen, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Box(modifier = Modifier.align(Alignment.BottomEnd).size(32.dp).background(Color.White, CircleShape).border(1.dp, Color.Gray, CircleShape).padding(6.dp)) {
                                Icon(Icons.Default.CameraAlt, null, tint = Color.Gray, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // TÊN NGƯỜI DÙNG
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showRenameDialog = true }
                    ) {
                        Text(
                            text = userInfo?.name ?: "Đang tải...",
                            fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Edit, null, tint = AppColors.TextGreen, modifier = Modifier.size(18.dp))
                    }

                    // EMAIL
                    Text(
                        text = userInfo?.email ?: "",
                        fontSize = 14.sp, color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // PHƯƠNG THỨC ĐĂNG NHẬP
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFE0F2F1),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Login, null, tint = Color(0xFF00695C), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Đăng nhập: ${userInfo?.provider}",
                                fontSize = 12.sp, color = Color(0xFF00695C), fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- THÔNG TIN THAM GIA & TÊN VỊT ---
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                // Card Tên Vịt
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().clickable { showDuckNameDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.duck_happy),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tên trợ lý Vịt", fontSize = 12.sp, color = Color(0xFFEF6C00))
                            Text(userInfo?.duckName ?: "Vịt con", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Icon(Icons.Default.Edit, null, tint = Color(0xFFEF6C00))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Card Ngày tham gia
                val joinedDate = userInfo?.createdAt ?: 0L
                val dateStr = if (joinedDate > 0L) {
                    SimpleDateFormat("dd 'tháng' MM, yyyy", Locale("vi", "VN")).format(joinedDate)
                } else "..."

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, tint = AppColors.TextGreen, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Thành viên từ", fontSize = 12.sp, color = Color.Gray)
                            Text(dateStr, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- THỐNG KÊ CÂY TRỒNG ---
            Text(
                text = "Thành tích làm vườn 🌳",
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.TextGreen,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val counts = userInfo?.treeCounts ?: emptyMap()
                TreeStatItem(SeedType.NORMAL.grownIcon, "Cây thường", counts[SeedType.NORMAL.id] ?: 0, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                TreeStatItem(SeedType.PINE.grownIcon, "Cây thông", counts[SeedType.PINE.id] ?: 0, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                TreeStatItem(SeedType.RED_LEAF.grownIcon, "Cây lá đỏ", counts[SeedType.RED_LEAF.id] ?: 0, Modifier.weight(1f))
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    // --- DIALOGS ---

    if (showRenameDialog) {
        ChangeNameDialog(
            currentName = userInfo?.name ?: "",
            onDismiss = { showRenameDialog = false },
            title = "Đổi tên người dùng",
            onConfirm = { newName ->
                authViewModel.updateUserName(newName,
                    onSuccess = {
                        authViewModel.loadUserInfo { i -> userInfo = i }
                        showRenameDialog = false
                    },
                    onError = { }
                )
            }
        )
    }

    if (showDuckNameDialog) {
        ChangeNameDialog(
            currentName = userInfo?.duckName ?: "",
            onDismiss = { showDuckNameDialog = false },
            title = "Đặt tên cho Vịt",
            onConfirm = { newName ->
                authViewModel.updateDuckName(newName,
                    onSuccess = {
                        authViewModel.loadUserInfo { i -> userInfo = i }
                        showDuckNameDialog = false
                    },
                    onError = { }
                )
            }
        )
    }
}

// --- COMPOSABLES PHỤ TRỢ (Để ở cuối file này để không bị lỗi Unresolved) ---

@Composable
fun TreeStatItem(iconRes: Int, name: String, count: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text(count.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.TextGreen)
            Text(name, fontSize = 11.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun ChangeNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    title: String = "Đổi tên",
    onConfirm: (String) -> Unit
) {
    var textState by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                label = { Text("Tên mới") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (textState.isNotBlank()) onConfirm(textState) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.ButtonGreen)
            ) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color.Gray) }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}