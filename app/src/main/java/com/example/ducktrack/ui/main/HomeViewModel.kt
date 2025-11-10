package com.example.ducktrack.ui.main

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ducktrack.data.LimitsStore
import com.example.ducktrack.data.UsageRepository
import com.example.ducktrack.data.model.AppUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = UsageRepository(app)
    private val limitsStore = LimitsStore(app)

    var usages: List<AppUsage> = emptyList(); private set
    var totalMs: Long = 0L; private set
    var limits: Map<String, Int> = emptyMap(); private set

    private val iconCache = HashMap<String, Drawable?>()

    fun iconFor(usage: AppUsage): Drawable? {
        val key = usage.iconPackage ?: return null
        return iconCache[key] ?: run {
            val d = try { getApplication<Application>().packageManager.getApplicationIcon(key) }
            catch (_: Exception) { null }
            iconCache[key] = d; d
        }
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repo.queryToday()
            val total = list.sumOf { it.totalForegroundMs }
            val allLimits = limitsStore.getAll()
            withContext(Dispatchers.Main) {
                usages = list
                totalMs = total
                limits = allLimits
            }
        }
    }

    fun setLimit(pkg: String, minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            limitsStore.setLimit(pkg, minutes)
            val m = limitsStore.getAll()
            withContext(Dispatchers.Main) { limits = m }
        }
    }
}
