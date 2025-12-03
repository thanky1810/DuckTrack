package com.example.ducktrack.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.ducktrack.R
import com.example.ducktrack.ui.theme.AppColors
import kotlinx.coroutines.launch

// Model dữ liệu cho từng trang
data class OnboardingPageData(
    val title: String,
    val description: String,
    val lottieResId: Int
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit // Hàm callback khi hoàn tất
) {
    val pages = listOf(
        OnboardingPageData(
            title = "Giành lại quyền kiểm soát",
            description = "Thời gian của bạn đang bị chiếm dụng bởi những lần mở ứng dụng vô thức? DuckTrack giúp bạn chặn các nguồn gây xao nhãng để tập trung vào điều thật sự quan trọng.",
            lottieResId = R.raw.anim_intro_block
        ),
        OnboardingPageData(
            title = "Kích hoạt trạng thái Tập trung",
            description = "Áp dụng Pomodoro kết hợp âm thanh thư giãn để đưa bạn vào quãng thời gian 'vàng' 25 phút—nơi hiệu suất được nâng lên mức tối đa.",
            lottieResId = R.raw.anim_intro_focus
        ),
        OnboardingPageData(
            title = "Biến nỗ lực thành thói quen tốt",
            description = "Mỗi phút bạn tập trung là một hạt giống được gieo. Hãy tưới nước cho khu vườn thói quen mỗi ngày để thấy bản thân trưởng thành rõ rệt.",
            lottieResId = R.raw.anim_intro_grow
        ),
        OnboardingPageData(
            title = "Hoàn thành mọi nhiệm vụ",
            description = "Sắp xếp công việc khoa học, theo dõi tiến độ rõ ràng. Khi tâm trí không còn rối bời, bạn sẽ dễ dàng hoàn thành mọi mục tiêu trong tầm tay.",
            lottieResId = R.raw.anim_intro_task
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            // --- Bottom Navigation ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { iteration ->
                        val isSelected = pagerState.currentPage == iteration
                        val width = if (isSelected) 24.dp else 8.dp
                        val color = if (isSelected) AppColors.ButtonGreen else Color(0xFFE0E0E0)

                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                // Nút "Tiếp theo" hoặc "Bắt đầu"
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onFinish()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.ButtonGreen),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage == pages.size - 1) "Bắt đầu" else "Tiếp theo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        // Sử dụng Box để xếp chồng nút Skip lên trên nội dung
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 2. Nội dung chính
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { index ->
                OnboardingPageContent(page = pages[index])
            }

            // 3. Nút SKIP
            // Chỉ hiện khi chưa đến trang cuối
            if (pagerState.currentPage < pages.size - 1) {
                TextButton(
                    onClick = onFinish,
                    modifier = Modifier
                        .align(Alignment.TopEnd) // Căn góc trên phải
                        .padding(top = 16.dp, end = 16.dp)
                ) {
                    Text(
                        text = "Bỏ qua",
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPageData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Hiển thị Animation Lottie ---
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(page.lottieResId))
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever // Lặp lại vô tận
        )

        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .size(280.dp) // Kích thước animation
                .padding(bottom = 40.dp)
        )

        // Tiêu đề
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextGreen,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mô tả
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}