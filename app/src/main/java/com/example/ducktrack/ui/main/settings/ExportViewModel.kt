package com.example.ducktrack.ui.main.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider // Quan trọng
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.utils.ExportHistoryManager
import com.example.ducktrack.utils.HistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ExportViewModel : ViewModel() {

    private val _historyList = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyList: StateFlow<List<HistoryItem>> = _historyList.asStateFlow()

    private val _latestItem = MutableStateFlow<HistoryItem?>(null)
    val latestItem: StateFlow<HistoryItem?> = _latestItem.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadHistory(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            val list = ExportHistoryManager.getHistory(context)
            _historyList.value = list
            _latestItem.value = list.firstOrNull()
            _isLoading.value = false
        }
    }

    // --- HÀM CHIA SẺ FILE (DÙNG FILEPROVIDER) ---
    fun shareFile(context: Context, item: HistoryItem) {
        try {
            val file = File(item.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "File không tồn tại! Hãy thử xuất lại.", Toast.LENGTH_SHORT).show()
                return
            }

            // Tạo URI an toàn để chia sẻ
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Báo cáo DuckTrack: ${item.fileName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Mở hộp thoại chia sẻ
            val chooser = Intent.createChooser(shareIntent, "Chia sẻ báo cáo")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Lỗi chia sẻ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun onExportSuccess(context: Context, filePath: String) {
        val file = File(filePath)
        val newItem = HistoryItem(
            fileName = file.name,
            filePath = filePath,
            dateModified = System.currentTimeMillis(),
            fileSize = "${file.length() / 1024} KB"
        )
        ExportHistoryManager.addFileToHistory(context, newItem)
        loadHistory(context)
    }
}