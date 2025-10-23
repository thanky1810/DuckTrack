package com.example.ducktrack.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.data.LimitsStore
import com.example.ducktrack.data.UsageRepository
import com.example.ducktrack.data.model.AppUsage
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = UsageRepository(app)
    private val limitsStore = LimitsStore(app)

    var usages: List<AppUsage> = emptyList()
        private set

    var limits: Map<String, Int> = emptyMap()
        private set

    fun load() = viewModelScope.launch {
        usages = repo.queryToday()
        limits = limitsStore.getAll()
    }

    fun setLimit(pkg: String, minutes: Int) = viewModelScope.launch {
        limitsStore.setLimit(pkg, minutes)
        limits = limitsStore.getAll()
    }

    val totalMs: Long
        get() = usages.sumOf { it.totalForegroundMs }
}
