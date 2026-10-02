package com.planecatcher.notify

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.planecatcher.core.radar.AlertPolicy
import com.planecatcher.data.local.CaughtPlaneDao
import com.planecatcher.data.prefs.UserPrefs
import com.planecatcher.di.ApplicationScope
import com.planecatcher.domain.RadarTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends one "plane nearby" alert per plane when it first enters range, honouring
 * the user's toggle and minimum tier. While the app is on screen the pop-up does
 * the job instead, so no notification is posted (the plane still counts as alerted).
 */
@Singleton
class AlertCoordinator @Inject constructor(
    private val tracker: RadarTracker,
    private val prefs: UserPrefs,
    private val caughtDao: CaughtPlaneDao,
    private val notifier: Notifier,
    @ApplicationScope private val scope: CoroutineScope,
) {
    /** Planes already alerted about in this process. */
    private val notified = mutableSetOf<String>()
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(tracker.state, prefs.settings, caughtDao.observeHexes()) { radar, settings, caught ->
                Triple(radar, settings, caught.toSet())
            }.collect { (radar, settings, caught) ->
                if (!settings.alertsEnabled || radar.lastUpdatedMs == null) return@collect
                val fresh = AlertPolicy.planesToAlert(radar.planes, settings.minAlertTier, caught, notified)
                if (fresh.isEmpty()) return@collect
                if (notified.size > MAX_REMEMBERED) notified.clear()
                notified += fresh.map { it.hex }
                val inForeground = withContext(Dispatchers.Main) {
                    ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                }
                if (!inForeground) fresh.forEach(notifier::notifyPlane)
            }
        }
    }

    private companion object {
        const val MAX_REMEMBERED = 5_000
    }
}
