package com.example.ducktrack.ui.main.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.MyApplication
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TasksViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MyApplication).repository

    private val _selectedDateMs = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMs: StateFlow<Long> = _selectedDateMs.asStateFlow()

    // --- 1. LẤY DỮ LIỆU TỪ CLOUD ---
    val tasks: StateFlow<List<TodoTask>> = _selectedDateMs.flatMapLatest { date ->
        repository.getTasksStream(date) // Hàm này giờ đã trỏ về Firestore
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ... (Giữ nguyên dateText, isToday...)
    val dateText: StateFlow<String> = _selectedDateMs.map { ms ->
        val date = Date(ms)
        val fmt = java.text.SimpleDateFormat("dd 'tháng' MM", Locale("vi", "VN"))
        val today = Date()
        val isToday = isSameDay(ms, today.time)
        if (isToday) "Hôm nay, ${fmt.format(date)}" else fmt.format(date)
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    val isToday: StateFlow<Boolean> = _selectedDateMs.map {
        isSameDay(it, System.currentTimeMillis())
    }.stateIn(viewModelScope, SharingStarted.Lazily, true)

    private val _selectedTaskIds = MutableStateFlow(emptySet<String>())
    val selectedTaskIds: StateFlow<Set<String>> = _selectedTaskIds.asStateFlow()

    private val _newTaskText = MutableStateFlow("")
    val newTaskText: StateFlow<String> = _newTaskText.asStateFlow()

    private val _taskToEdit = MutableStateFlow<TodoTask?>(null)
    val taskToEdit: StateFlow<TodoTask?> = _taskToEdit.asStateFlow()

    fun previousDay() {
        val cal = Calendar.getInstance().apply { timeInMillis = _selectedDateMs.value }
        cal.add(Calendar.DAY_OF_YEAR, -1)
        _selectedDateMs.value = cal.timeInMillis
        _selectedTaskIds.value = emptySet()
    }
    fun nextDay() {
        val cal = Calendar.getInstance().apply { timeInMillis = _selectedDateMs.value }
        cal.add(Calendar.DAY_OF_YEAR, 1)
        _selectedDateMs.value = cal.timeInMillis
        _selectedTaskIds.value = emptySet()
    }

    fun onNewTaskTextChange(text: String) { _newTaskText.value = text }

    // --- 2. THÊM TASK LÊN CLOUD ---
    fun onAddTask() {
        val text = _newTaskText.value
        if (text.isNotBlank()) {
            viewModelScope.launch {
                repository.addTaskToCloud(text, _selectedDateMs.value)
                _newTaskText.value = ""
            }
        }
    }

    fun onTaskClick(task: TodoTask) {
        if (_selectedTaskIds.value.isNotEmpty()) onToggleSelection(task.id)
        else onToggleComplete(task)
    }
    fun onTaskLongPress(task: TodoTask) { onToggleSelection(task.id) }
    fun onCloseSelectionMode() { _selectedTaskIds.value = emptySet() }

    // --- 3. XÓA TASK TRÊN CLOUD ---
    fun onDeleteSelected() {
        viewModelScope.launch {
            repository.deleteMultipleTasksCloud(_selectedTaskIds.value)
            _selectedTaskIds.value = emptySet()
        }
    }

    // --- 4. GHIM TASK TRÊN CLOUD ---
    fun onPinSelected() {
        viewModelScope.launch {
            repository.pinMultipleTasksCloud(_selectedTaskIds.value)
            _selectedTaskIds.value = emptySet()
        }
    }

    // --- 5. BỎ GHIM TRÊN CLOUD ---
    fun onUnpinClick(task: TodoTask) {
        viewModelScope.launch {
            repository.updateTaskInCloud(task.copy(isPinned = false))
        }
    }

    // --- 6. TICK HOÀN THÀNH TRÊN CLOUD ---
    private fun onToggleComplete(task: TodoTask) {
        viewModelScope.launch {
            repository.updateTaskInCloud(task.copy(isCompleted = !task.isCompleted))
        }
    }

    private fun onToggleSelection(taskId: String) {
        _selectedTaskIds.update { currentSet ->
            currentSet.toMutableSet().apply {
                if (contains(taskId)) remove(taskId) else add(taskId)
            }
        }
    }

    fun onEditClick(task: TodoTask) { _taskToEdit.value = task }
    fun onCancelEdit() { _taskToEdit.value = null }

    // --- 7. SỬA NỘI DUNG TRÊN CLOUD ---
    fun onConfirmEdit(newDescription: String) {
        _taskToEdit.value?.let { task ->
            if (newDescription.isNotBlank() && newDescription != task.description) {
                viewModelScope.launch {
                    repository.updateTaskInCloud(task.copy(description = newDescription))
                }
            }
        }
        _taskToEdit.value = null
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }
}