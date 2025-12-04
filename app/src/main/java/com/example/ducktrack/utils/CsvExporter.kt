// FILE: utils/CsvExporter.kt
package com.example.ducktrack.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.ducktrack.ui.UserInfo
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.example.ducktrack.ui.main.tasks.TaskStats

// Model giữ nguyên
data class TreeHistoryCsvItem(
    val treeType: String,
    val timestamp: Long,
    val configParam: String,
    val status: String
)
data class AppUsageCsvItem(val appName: String, val totalTimeInMonthMs: Long)

object CsvExporter {

    // Hàm tạo tên file
    fun generateFileName(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "ducktrack_report_$timeStamp.csv"
    }

    // CHỈ CÒN 1 HÀM DUY NHẤT: LƯU VÀO DOWNLOAD/DUCKTRACK
    fun saveToDownloads(
        context: Context,
        userInfo: UserInfo,
        weeklyTrees: List<TreeHistoryCsvItem>,
        monthlyApps: List<AppUsageCsvItem>,
        taskStats: TaskStats // <--- THÊM MỚI
    ): String? {
        val fileName = generateFileName()
        // Truyền taskStats xuống hàm build
        val csvContent = buildCsvContent(userInfo, weeklyTrees, monthlyApps, taskStats)

        return try {
            // ... (Giữ nguyên logic lưu file MediaStore/File cũ)
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
                // Trả về đường dẫn tương đối để hiển thị (vì Android 10+ không có path thật dễ dàng)
                "Download/DuckTrack/$fileName"
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

    // SỬA HÀM NÀY: Thêm logic ghi thống kê Task
    private fun buildCsvContent(
        userInfo: UserInfo,
        weeklyTrees: List<TreeHistoryCsvItem>,
        monthlyApps: List<AppUsageCsvItem>,
        taskStats: TaskStats // <--- THÊM MỚI
    ): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        sb.append("=== THÔNG TIN NGƯỜI DÙNG ===\n")
        sb.append("Tên,Email,Vịt cưng,Ngày tạo\n")
        val createdDate = dateFormat.format(Date(userInfo.createdAt))
        sb.append("${userInfo.name},${userInfo.email},${userInfo.duckName},$createdDate\n\n")

        // --- THÊM ĐOẠN NÀY ---
        sb.append("=== THỐNG KÊ NHIỆM VỤ ===\n")
        sb.append("Tổng số nhiệm vụ,Đã hoàn thành,Chưa hoàn thành\n")
        sb.append("${taskStats.total},${taskStats.completed},${taskStats.pending}\n\n")
        // ---------------------

        sb.append("=== LỊCH SỬ TRỒNG CÂY (TUẦN NÀY) ===\n")
        // ... (Giữ nguyên phần còn lại)
        sb.append("Loại cây,Thời gian,Thông số (Config),Kết quả\n")
        if (weeklyTrees.isEmpty()) sb.append("Chưa có dữ liệu trồng cây tuần này,,,\n")
        else weeklyTrees.forEach { tree ->
            val timeStr = dateFormat.format(Date(tree.timestamp))
            sb.append("${tree.treeType},$timeStr,${tree.configParam},${tree.status}\n")
        }
        sb.append("\n")

        sb.append("=== THỐNG KÊ SỬ DỤNG ĐIỆN THOẠI (30 NGÀY) ===\n")
        sb.append("Tên Ứng Dụng,Thời Gian Sử Dụng\n")
        if (monthlyApps.isEmpty()) sb.append("Không có dữ liệu usage stats,0\n")
        else monthlyApps.forEach { app ->
            val timeStr = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(app.totalTimeInMonthMs), TimeUnit.MILLISECONDS.toMinutes(app.totalTimeInMonthMs) % 60, TimeUnit.MILLISECONDS.toSeconds(app.totalTimeInMonthMs) % 60)
            sb.append("${app.appName},$timeStr\n")
        }
        return sb.toString()
    }
}