
package com.example.ducktrack.ui.main.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TasksViewModel(application: Application) : AndroidViewModel(application) {

    private val taskDao = TaskDatabase.getDatabase(application).taskDao()

    // --- State cho Giao diện ---
    val tasks: StateFlow<List<Task>> = taskDao.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedTaskIds = MutableStateFlow(emptySet<Int>())
    val selectedTaskIds: StateFlow<Set<Int>> = _selectedTaskIds.asStateFlow()

    private val _newTaskText = MutableStateFlow("")
    val newTaskText: StateFlow<String> = _newTaskText.asStateFlow()

    private val _taskToEdit = MutableStateFlow<Task?>(null)
    val taskToEdit: StateFlow<Task?> = _taskToEdit.asStateFlow()

    // --- Các hành động (Events) từ Giao diện ---

    fun onNewTaskTextChange(text: String) {
        _newTaskText.value = text
    }

    fun onAddTask() {
        val text = _newTaskText.value
        if (text.isNotBlank()) {
            viewModelScope.launch {
                taskDao.insertTask(
                    Task(
                        description = text,
                        isCompleted = false,
                        isPinned = false
                    )
                )
                _newTaskText.value = ""
            }
        }
    }

    fun onTaskClick(task: Task) {
        if (_selectedTaskIds.value.isNotEmpty()) {
            onToggleSelection(task.id)
        } else {
            onToggleComplete(task)
        }
    }

    fun onTaskLongPress(task: Task) {
        onToggleSelection(task.id)
    }

    fun onCloseSelectionMode() {
        _selectedTaskIds.value = emptySet()
    }

    fun onDeleteSelected() {
        viewModelScope.launch {
            taskDao.deleteTasks(_selectedTaskIds.value)
            _selectedTaskIds.value = emptySet()
        }
    }

    fun onPinSelected() {
        viewModelScope.launch {
            taskDao.pinTasks(_selectedTaskIds.value)
            _selectedTaskIds.value = emptySet()
        }
    }

    fun onUnpinClick(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task.copy(isPinned = false))
        }
    }

    private fun onToggleComplete(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    private fun onToggleSelection(taskId: Int) {
        _selectedTaskIds.update { currentSet ->
            currentSet.toMutableSet().apply {
                if (contains(taskId)) remove(taskId)
                else add(taskId)
            }
        }
    }

    // --- Các hàm xử lý Sửa Task ---

    fun onEditClick(task: Task) {
        _taskToEdit.value = task
    }

    fun onCancelEdit() {
        _taskToEdit.value = null
    }

    fun onConfirmEdit(newDescription: String) {
        _taskToEdit.value?.let { task ->
            if (newDescription.isNotBlank() && newDescription != task.description) {
                viewModelScope.launch {
                    taskDao.updateTask(task.copy(description = newDescription))
                }
            }
        }
        _taskToEdit.value = null
    }
}