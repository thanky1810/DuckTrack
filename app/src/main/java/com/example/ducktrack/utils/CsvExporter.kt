package com.example.ducktrack.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.ducktrack.ui.UserInfo
import com.example.ducktrack.ui.main.tasks.TaskStats
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- CẬP NHẬT MODEL: THÊM setIndex VÀ sessionIndex ---
data class TreeHistoryCsvItem(
    val treeType: String,
    val timestamp: Long,
    val configParam: String,
    val status: String,
    // Thêm 2 trường này (Mặc định là 0 nếu dữ liệu cũ không có)
    val setIndex: Int = 0,
    val sessionIndex: Int = 0
)

data class AppUsageCsvItem(val appName: String, val totalTimeInMonthMs: Long)

object CsvExporter {

    fun generateFileName(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "ducktrack_report_$timeStamp.csv"
    }

    fun saveToDownloads(
        context: Context,
        userInfo: UserInfo,
        weeklyTrees: List<TreeHistoryCsvItem>,
        monthlyApps: List<AppUsageCsvItem>,
        taskStats: TaskStats,
        treeCounts: Map<String, Int>
    ): String? {
        val fileName = generateFileName()
        val csvContent = buildCsvContent(userInfo, weeklyTrees, monthlyApps, taskStats, treeCounts)

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DuckTrack")
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("Không thể tạo file MediaStore")

                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(0xEF); outputStream.write(0xBB); outputStream.write(0xBF)
                    outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                }

                // Trả về đường dẫn tuyệt đối chuẩn xác
                val absolutePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/DuckTrack/" + fileName
                absolutePath

            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val appDir = File(downloadsDir, "DuckTrack")
                if (!appDir.exists()) appDir.mkdirs()

                val file = File(appDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(0xEF); outputStream.write(0xBB); outputStream.write(0xBF)
                    outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                }
                file.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildCsvContent(
        userInfo: UserInfo,
        weeklyTrees: List<TreeHistoryCsvItem>,
        monthlyApps: List<AppUsageCsvItem>,
        taskStats: TaskStats,
        treeCounts: Map<String, Int>
    ): String {
        val sb = StringBuilder()
        val fullDateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm:ss aa", Locale("vi", "VN"))
        val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        sb.append("=== THÔNG TIN NGƯỜI DÙNG ===\n")
        sb.append("Tên,Email,Vịt cưng,Ngày tạo\n")
        val createdDate = simpleDateFormat.format(Date(userInfo.createdAt))
        sb.append("${userInfo.name},${userInfo.email},${userInfo.duckName},$createdDate\n\n")

        sb.append("=== THỐNG KÊ NHIỆM VỤ TỔNG QUÁT ===\n")
        sb.append("Tổng số nhiệm vụ,Đã hoàn thành,Chưa hoàn thành\n")
        sb.append("${taskStats.total},${taskStats.completed},${taskStats.pending}\n\n")

        sb.append("=== CHI TIẾT THEO PHƯƠNG PHÁP EISENHOWER ===\n")
        sb.append("Loại thẻ (Mức độ),Tổng số,Đã hoàn thành,Chưa hoàn thành\n")
        if (taskStats.details.isEmpty()) {
            sb.append("Chưa có dữ liệu chi tiết,,,\n")
        } else {
            taskStats.details.forEach { q ->
                sb.append("${q.name},${q.total},${q.completed},${q.pending}\n")
            }
        }
        sb.append("\n")

        sb.append("=== THỐNG KÊ SỐ LƯỢNG CÂY ĐÃ TRỒNG ===\n")
        sb.append("Loại cây,Số lượng\n")
        if (treeCounts.isEmpty()) {
            sb.append("Chưa trồng cây nào,0\n")
        } else {
            treeCounts.forEach { (name, count) ->
                sb.append("$name,$count\n")
            }
        }
        sb.append("\n")

        sb.append("=== LỊCH SỬ TRỒNG CÂY CHI TIẾT (TUẦN NÀY) ===\n")
        // Thêm cột Bộ phiên và Thứ tự
        sb.append("Loại cây,Thời gian,Bộ phiên,Thứ tự,Thông số (Config),Kết quả\n")

        if (weeklyTrees.isEmpty()) {
            sb.append("Chưa có dữ liệu trồng cây tuần này,,,,,\n")
        } else {
            weeklyTrees.forEach { tree ->
                val timeStr = fullDateFormat.format(Date(tree.timestamp))

                // Tách chuỗi config "25/5/4/15" để lấy số 4 (tổng số phiên)
                val totalSessions = try {
                    val parts = tree.configParam.split("/")
                    if (parts.size >= 3) parts[2] else "?"
                } catch (e: Exception) { "?" }

                // Ghi dữ liệu: setIndex và sessionIndex lấy từ object tree
                val setStr = if (tree.setIndex > 0) "Bộ ${tree.setIndex}" else "-"
                val sessionStr = if (tree.sessionIndex > 0) "${tree.sessionIndex}/$totalSessions" else "-"

                sb.append("${tree.treeType},$timeStr,$setStr,$sessionStr,${tree.configParam},${tree.status}\n")
            }
        }
        sb.append("\n")

        sb.append("=== THỐNG KÊ SỬ DỤNG ĐIỆN THOẠI (30 NGÀY) ===\n")
        sb.append("Tên Ứng Dụng,Thời Gian Sử Dụng\n")
        if (monthlyApps.isEmpty()) {
            sb.append("Không có dữ liệu usage stats,0\n")
        } else {
            monthlyApps.forEach { app ->
                val timeStr = String.format(
                    "%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(app.totalTimeInMonthMs),
                    TimeUnit.MILLISECONDS.toMinutes(app.totalTimeInMonthMs) % 60,
                    TimeUnit.MILLISECONDS.toSeconds(app.totalTimeInMonthMs) % 60
                )
                sb.append("${app.appName},$timeStr\n")
            }
        }
        return sb.toString()
    }
}