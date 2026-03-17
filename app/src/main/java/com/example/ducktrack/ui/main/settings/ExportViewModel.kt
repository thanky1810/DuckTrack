package com.example.ducktrack.ui.main.settings

import android.content.Context
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

    fun loadHistory(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            val list = ExportHistoryManager.getHistory(context)
            _historyList.value = list
            _latestItem.value = list.firstOrNull()
            _isLoading.value = false
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