package com.planecatcher.service

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.planecatcher.core.rules.JumpStatus
import com.planecatcher.data.location.LocationSource
import com.planecatcher.domain.JumpManager
import com.planecatcher.domain.RadarTracker
import com.planecatcher.notify.Notifier
import com.planecatcher.ui.common.formatDuration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service with an ongoing notification that keeps the radar (location +
 * polling) and alerts alive while the app is in the background. Uses foreground
 * location only; no ACCESS_BACKGROUND_LOCATION.
 */
@AndroidEntryPoint
class RadarService : LifecycleService() {
    @Inject lateinit var tracker: RadarTracker
    @Inject lateinit var notifier: Notifier
    @Inject lateinit var jump: JumpManager
    @Inject lateinit var location: LocationSource

    private var holding = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!location.hasPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        ServiceCompat.startForeground(
            this, Notifier.RADAR_NOTIFICATION_ID, notifier.radarNotification("Looking for planes…"), type,
        )
        if (!holding) {
            holding = true
            tracker.acquire()
            lifecycleScope.launch {
                combine(tracker.state, jump.status) { radar, jumpStatus ->
                    val planes = radar.planes.size
                    val base = when {
                        radar.center == null -> "Waiting for location"
                        planes == 0 -> "No planes in range"
                        planes == 1 -> "1 plane in range"
                        else -> "$planes planes in range"
                    }
                    if (jumpStatus is JumpStatus.Active) {
                        "$base at ${jumpStatus.target.code} · jump ends in ${formatDuration(jumpStatus.remainingMs)}"
                    } else {
                        base
                    }
                }.collect { text ->
                    val nm = getSystemService(android.app.NotificationManager::class.java)
                    if (notifier.canPost()) nm.notify(Notifier.RADAR_NOTIFICATION_ID, notifier.radarNotification(text))
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (holding) tracker.release()
        holding = false
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, RadarService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RadarService::class.java))
        }
    }
}
