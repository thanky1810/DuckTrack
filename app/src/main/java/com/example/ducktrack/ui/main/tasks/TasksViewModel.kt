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

// Enum để quản lý chế độ xem
enum class TaskViewMode {
    LIST, EISENHOWER
}

class TasksViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MyApplication).repository

    private val _selectedDateMs = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMs: StateFlow<Long> = _selectedDateMs.asStateFlow()

    // --- STATE QUẢN LÝ CHẾ ĐỘ XEM ---
    private val _viewMode = MutableStateFlow(TaskViewMode.LIST)
    val viewMode: StateFlow<TaskViewMode> = _viewMode.asStateFlow()

    val tasks: StateFlow<List<TodoTask>> = _selectedDateMs.flatMapLatest { date ->
        repository.getTasksStream(date)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ... (Giữ nguyên dateText, isToday, _selectedTaskIds...)
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

    // --- HÀM CHUYỂN ĐỔI CHẾ ĐỘ ---
    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == TaskViewMode.LIST) TaskViewMode.EISENHOWER else TaskViewMode.LIST
    }

    // ... (Giữ nguyên previousDay, nextDay, onNewTaskTextChange...)
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

    // --- CẬP NHẬT HÀM THÊM TASK ---
    // Mặc định thêm vào List thường (Important=false, Urgent=false)
    // Nhưng nếu đang ở Eisenhower mode, có thể ta cần logic khác (sẽ xử lý ở UI gọi hàm riêng)
    fun onAddTask(isImportant: Boolean = false, isUrgent: Boolean = false) {
        val text = _newTaskText.value
        if (text.isNotBlank()) {
            viewModelScope.launch {
                repository.addTaskToCloud(text, _selectedDateMs.value, isImportant, isUrgent)
                _newTaskText.value = ""
            }
        }
    }

    // ... (Giữ nguyên onTaskClick, onTaskLongPress, onCloseSelectionMode, onDeleteSelected, onPinSelected, onUnpinClick...)
    fun onTaskClick(task: TodoTask) {
        if (_selectedTaskIds.value.isNotEmpty()) onToggleSelection(task.id)
        else onToggleComplete(task)
    }
    fun onTaskLongPress(task: TodoTask) { onToggleSelection(task.id) }
    fun onCloseSelectionMode() { _selectedTaskIds.value = emptySet() }
    fun onDeleteSelected() {
        viewModelScope.launch {
            repository.deleteMultipleTasksCloud(_selectedTaskIds.value)
            _selectedTaskIds.value = emptySet()
        }
    }
    fun onPinSelected() {
        viewModelScope.launch {
            repository.pinMultipleTasksCloud(_selectedTaskIds.value)
            _selectedTaskIds.value = emptySet()
        }
    }
    fun onUnpinClick(task: TodoTask) {
        viewModelScope.launch { repository.updateTaskInCloud(task.copy(isPinned = false)) }
    }
    private fun onToggleComplete(task: TodoTask) {
        viewModelScope.launch { repository.updateTaskInCloud(task.copy(isCompleted = !task.isCompleted)) }
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