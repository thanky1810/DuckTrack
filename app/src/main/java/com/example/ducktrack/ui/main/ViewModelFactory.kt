package com.example.ducktrack.ui.main

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ducktrack.ui.main.garden.GardenViewModel
import com.example.ducktrack.ui.main.pomodoro.PomodoroViewModel

/**
 * Factory này "dạy" hệ thống cách tạo AndroidViewModel (như PomodoroViewModel
 * và GardenViewModel) khi chúng cần một Application.
 */
class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(PomodoroViewModel::class.java) -> {
                PomodoroViewModel(application) as T
            }

            modelClass.isAssignableFrom(GardenViewModel::class.java) -> {
                GardenViewModel(application) as T
            }
            // Cho các ViewModel khác
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}