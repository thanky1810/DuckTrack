package com.example.ducktrack.ui.main.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.NaturePeople
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.vector.ImageVector

enum class AchievementCategory(val title: String) {
    TASK("Nhiệm vụ"),
    GARDEN("Vườn cây"),
    FOCUS("Sự tập trung"),
    PROFILE("Hồ sơ & Khác"),
    MASTER("Huyền thoại")
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val type: AchievementType,
    val category: AchievementCategory, // Thêm cái này để phân nhóm UI
    val icon: ImageVector
)

enum class AchievementType {
    TASK_COMPLETED, // Tổng số task đã làm
    TASK_PINNED,    // Số task đang ghim (check tại thời điểm)
    TREE_COUNT,     // Tổng số cây
    PINE_LOVER,     // Số cây thông
    RED_LEAF_LOVER, // Số cây lá đỏ
    FOCUS_SESSION,  // Số phiên tập trung (tính bằng số cây)
    PROFILE_UPDATE, // Đã đổi tên hoặc avatar
    MASTER_ALL      // Thành tựu cuối cùng
}

object AchievementList {
    val list = listOf(
        // --- 1. NHÓM NHIỆM VỤ (8 cái) ---
        Achievement(
            "task_1",
            "Khởi đầu",
            "Hoàn thành 1 nhiệm vụ",
            1,
            AchievementType.TASK_COMPLETED,
            AchievementCategory.TASK,
            Icons.Default.Check
        ),
        Achievement(
            "task_10",
            "Người bận rộn",
            "Hoàn thành 10 nhiệm vụ",
            10,
            AchievementType.TASK_COMPLETED,
            AchievementCategory.TASK,
            Icons.AutoMirrored.Filled.List
        ),
        Achievement(
            "task_50",
            "Cỗ máy công việc",
            "Hoàn thành 50 nhiệm vụ",
            50,
            AchievementType.TASK_COMPLETED,
            AchievementCategory.TASK,
            Icons.Default.AssignmentTurnedIn
        ),
        Achievement(
            "task_100",
            "Chuyên gia",
            "Hoàn thành 100 nhiệm vụ",
            100,
            AchievementType.TASK_COMPLETED,
            AchievementCategory.TASK,
            Icons.Default.WorkspacePremium
        ),
        Achievement(
            "task_200",
            "Bậc thầy",
            "Hoàn thành 200 nhiệm vụ",
            200,
            AchievementType.TASK_COMPLETED,
            AchievementCategory.TASK,
            Icons.Default.Verified
        ),
        Achievement(
            "task_500",
            "Thần thánh",
            "Hoàn thành 500 nhiệm vụ",
            500,
            AchievementType.TASK_COMPLETED,
            AchievementCategory.TASK,
            Icons.Default.AutoAwesome
        ),
        Achievement(
            "pin_1",
            "Ghim việc",
            "Ghim ít nhất 1 nhiệm vụ quan trọng",
            1,
            AchievementType.TASK_PINNED,
            AchievementCategory.TASK,
            Icons.Default.PushPin
        ),
        Achievement(
            "pin_5",
            "Ưu tiên cao",
            "Ghim cùng lúc 5 nhiệm vụ",
            5,
            AchievementType.TASK_PINNED,
            AchievementCategory.TASK,
            Icons.Default.Bookmarks
        ),

        // --- 2. NHÓM VƯỜN CÂY (8 cái) ---
        Achievement(
            "tree_1",
            "Mầm non",
            "Trồng cây đầu tiên",
            1,
            AchievementType.TREE_COUNT,
            AchievementCategory.GARDEN,
            Icons.Default.Grass
        ),
        Achievement(
            "tree_10",
            "Khu vườn nhỏ",
            "Trồng 10 cây",
            10,
            AchievementType.TREE_COUNT,
            AchievementCategory.GARDEN,
            Icons.Default.Yard
        ),
        Achievement(
            "tree_50",
            "Rừng rậm",
            "Trồng 50 cây",
            50,
            AchievementType.TREE_COUNT,
            AchievementCategory.GARDEN,
            Icons.Default.Forest
        ),
        Achievement(
            "tree_100",
            "Lá phổi xanh",
            "Trồng 100 cây",
            100,
            AchievementType.TREE_COUNT,
            AchievementCategory.GARDEN,
            Icons.Default.NaturePeople
        ),
        Achievement(
            "pine_5",
            "Yêu cây thông",
            "Trồng 5 cây Thông",
            5,
            AchievementType.PINE_LOVER,
            AchievementCategory.GARDEN,
            Icons.Default.Park
        ),
        Achievement(
            "pine_20",
            "Rừng thông",
            "Trồng 20 cây Thông",
            20,
            AchievementType.PINE_LOVER,
            AchievementCategory.GARDEN,
            Icons.Default.Park
        ),
        Achievement(
            "red_5",
            "Mùa thu",
            "Trồng 5 cây Lá đỏ",
            5,
            AchievementType.RED_LEAF_LOVER,
            AchievementCategory.GARDEN,
            Icons.Default.Park
        ),
        Achievement(
            "red_20",
            "Rừng lá đỏ",
            "Trồng 20 cây Lá đỏ",
            20,
            AchievementType.RED_LEAF_LOVER,
            AchievementCategory.GARDEN,
            Icons.Default.Park
        ),

        // --- 3. SỰ TẬP TRUNG (8 cái) ---
        // Giả sử 1 cây = 1 phiên tập trung (25p)
        Achievement(
            "focus_5",
            "Tập sự",
            "Hoàn thành 5 phiên",
            5,
            AchievementType.FOCUS_SESSION,
            AchievementCategory.FOCUS,
            Icons.Default.Timer
        ),
        Achievement(
            "focus_25",
            "Quen nhịp",
            "Hoàn thành 25 phiên",
            25,
            AchievementType.FOCUS_SESSION,
            AchievementCategory.FOCUS,
            Icons.Default.HourglassBottom
        ),
        Achievement(
            "focus_50",
            "Siêu tập trung",
            "Hoàn thành 50 phiên",
            50,
            AchievementType.FOCUS_SESSION,
            AchievementCategory.FOCUS,
            Icons.Default.Psychology
        ),
        Achievement(
            "focus_100",
            "Bậc thầy thời gian",
            "Hoàn thành 100 phiên",
            100,
            AchievementType.FOCUS_SESSION,
            AchievementCategory.FOCUS,
            Icons.Default.AccessTimeFilled
        ),
        Achievement(
            "focus_200",
            "Thiền định",
            "Hoàn thành 200 phiên",
            200,
            AchievementType.FOCUS_SESSION,
            AchievementCategory.FOCUS,
            Icons.Default.SelfImprovement
        ),
        Achievement(
            "focus_300",
            "Không xao nhãng",
            "Hoàn thành 300 phiên",
            300,
            AchievementType.FOCUS_SESSION,
            AchievementCategory.FOCUS,
            Icons.Default.DoNotDisturbOn
        ),
        Achievement(
            "focus_400",
            "Kiên định",
            "Hoàn thành 400 phiên",
            400,
            AchievementType.FOCUS_SESSION,
            AchievementCategory.FOCUS,
            Icons.Default.Spa
        ),
        Achievement(
            "focus_500",
            "Hòa làm một",
            "Hoàn thành 500 phiên",
            500,
            AchievementType.FOCUS_SESSION,
            AchievementCategory.FOCUS,
            Icons.Default.WbSunny
        ),
        // --- 4. HỒ SƠ (6 cái) ---
        Achievement(
            "profile_duck",
            "Đặt tên Vịt",
            "Đổi tên cho trợ lý Vịt",
            1,
            AchievementType.PROFILE_UPDATE,
            AchievementCategory.PROFILE,
            Icons.Default.Pets
        ),
        Achievement(
            "profile_name",
            "Danh tính",
            "Cập nhật tên hiển thị",
            1,
            AchievementType.PROFILE_UPDATE,
            AchievementCategory.PROFILE,
            Icons.Default.Badge
        ),
        Achievement(
            "profile_avatar",
            "Gương mặt mới",
            "Cập nhật ảnh đại diện",
            1,
            AchievementType.PROFILE_UPDATE,
            AchievementCategory.PROFILE,
            Icons.Default.Face
        ),
        // Các mốc tổng hợp nhỏ
        Achievement(
            "total_10",
            "Khởi động",
            "Mở khóa 10 thành tựu bất kỳ",
            10,
            AchievementType.MASTER_ALL,
            AchievementCategory.PROFILE,
            Icons.AutoMirrored.Filled.StarHalf
        ),
        Achievement(
            "total_20",
            "Tăng tốc",
            "Mở khóa 20 thành tựu bất kỳ",
            20,
            AchievementType.MASTER_ALL,
            AchievementCategory.PROFILE,
            Icons.Default.Star
        ),
        Achievement(
            "total_29",
            "Gần đích",
            "Mở khóa 29 thành tựu",
            29,
            AchievementType.MASTER_ALL,
            AchievementCategory.PROFILE,
            Icons.Default.Stars
        ),

        // --- 5. CUỐI CÙNG (1 cái) ---
        Achievement(
            id = "master_complete",
            title = "VỊT VÀNG HUYỀN THOẠI",
            description = "Mở khóa toàn bộ 30 thành tựu trên. Bạn là đỉnh của chóp!",
            target = 30,
            type = AchievementType.MASTER_ALL,
            category = AchievementCategory.MASTER,
            icon = Icons.Default.EmojiEvents // Cúp vàng
        )
    )
}