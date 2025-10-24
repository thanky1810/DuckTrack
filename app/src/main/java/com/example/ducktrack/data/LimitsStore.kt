package com.example.ducktrack.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

// Extension DataStore phải ở top-level (không đặt trong class)
val Context.limitsDataStore by preferencesDataStore("limits_store")

class LimitsStore(private val ctx: Context) {

    private object Keys { val LIMITS = stringPreferencesKey("limits_json") }

    // Ghi: map<packageName, minutes>
    suspend fun setLimit(pkg: String, minutes: Int) {
        ctx.limitsDataStore.edit { pref ->
            val current = pref[Keys.LIMITS] ?: "{}"
            val map = JSONObject(current)
            map.put(pkg, minutes)
            pref[Keys.LIMITS] = map.toString()
        }
    }

    suspend fun getAll(): Map<String, Int> {
        val obj = ctx.limitsDataStore.data
            .map { pref -> JSONObject(pref[Keys.LIMITS] ?: "{}") }
            .first()
        return obj.keys().asSequence().associateWith { obj.getInt(it) }
    }

    suspend fun getLimit(pkg: String): Int? {
        val all = getAll()
        return all[pkg]
    }
}
