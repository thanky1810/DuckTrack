package com.example.ducktrack.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.ducktrack.data.LimitsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Khi máy khởi động xong, nếu user có limit + đang bật giám sát thì start service
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                val limitsStore = LimitsStore(context)

                val hasAnyLimit = limitsStore.getAll().isNotEmpty()
                val monitoringEnabled = limitsStore.isMonitoringEnabled.first()

                if (hasAnyLimit && monitoringEnabled) {
                    startMonitoringService(context)
                }
            }
        }
    }

    private fun startMonitoringService(context: Context) {
        val serviceIntent = Intent(context, UsageMonitorService::class.java).apply {
            action = UsageMonitorService.ACTION_START_MONITORING
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
