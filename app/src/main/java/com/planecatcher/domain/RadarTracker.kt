package com.planecatcher.domain

import android.os.SystemClock
import com.planecatcher.core.model.GeoPoint
import com.planecatcher.core.radar.NearbyPlane
import com.planecatcher.core.radar.PollSchedule
import com.planecatcher.core.radar.Radar
import com.planecatcher.core.rules.GameRules
import com.planecatcher.core.rules.JumpStatus
import com.planecatcher.core.rules.JumpTarget
import com.planecatcher.core.rules.LocationJump
import com.planecatcher.data.PlaneDataException
import com.planecatcher.data.PlaneRepository
import com.planecatcher.data.TierProvider
import com.planecatcher.data.local.NearbyCacheDao
import com.planecatcher.data.local.NearbyPlaneCacheEntity
import com.planecatcher.data.location.DeviceLocation
import com.planecatcher.data.location.LocationSource
import com.planecatcher.data.prefs.UserPrefs
import com.planecatcher.data.time.TrustedClock
import com.planecatcher.di.ApplicationScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class RadarState(
    val center: GeoPoint? = null,
    /** Non-null while a Location Jump is moving the radar. */
    val jumpTarget: JumpTarget? = null,
    val planes: List<NearbyPlane> = emptyList(),
    val lastUpdatedMs: Long? = null,
    val error: String? = null,
    val running: Boolean = false,
    val hasLocationPermission: Boolean = true,
    val mockLocation: Boolean = false,
)

/**
 * Polls the ADS-B data source around the radar centre (device location, or the jump target
 * while a Location Jump is active) and publishes the planes within catch range.
 *
 * It runs only while someone holds it: the visible Home screen, or the background
 * radar service. That keeps polling off when nothing needs it (battery).
 */
@Singleton
class RadarTracker @Inject constructor(
    private val location: LocationSource,
    private val repository: PlaneRepository,
    private val prefs: UserPrefs,
    private val tiers: TierProvider,
    private val cacheDao: NearbyCacheDao,
    private val clock: TrustedClock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(RadarState())
    val state: StateFlow<RadarState> = _state.asStateFlow()

    private val refreshSignal = Channel<Unit>(Channel.CONFLATED)
    private var holders = 0
    private var job: Job? = null
    private var lastRequestElapsed = 0L

    @Synchronized
    fun acquire() {
        holders++
        if (job == null) job = scope.launch { run() }
    }

    @Synchronized
    fun release() {
        holders = (holders - 1).coerceAtLeast(0)
        if (holders == 0) {
            job?.cancel()
            job = null
            _state.update { it.copy(running = false) }
        }
    }

    /** Poll as soon as allowed (e.g. after a jump starts or ends). */
    fun refreshNow() {
        refreshSignal.trySend(Unit)
    }

    private suspend fun run() = coroutineScope {
        val device = MutableStateFlow<DeviceLocation?>(null)
        _state.update { it.copy(running = true, hasLocationPermission = location.hasPermission()) }
        var locationJob = launch { location.updates().collect { device.value = it } }

        while (isActive) {
            // Location permission may be granted after the radar started.
            if (locationJob.isCompleted && location.hasPermission()) {
                locationJob = launch { location.updates().collect { device.value = it } }
            }
            val now = clock.now()
            val storedJump = prefs.jumpState.first()
            val jump = LocationJump.normalize(storedJump, now)
            if (jump != storedJump) prefs.setJumpState(jump) // an expired jump has ended
            val activeJump = (LocationJump.status(jump, now) as? JumpStatus.Active)?.target

            val dev = device.value
            val center = activeJump?.point ?: dev?.point
            var failed = false

            if (center == null) {
                _state.update {
                    it.copy(
                        center = null, jumpTarget = null, planes = emptyList(),
                        hasLocationPermission = location.hasPermission(),
                    )
                }
            } else {
                // Respect the ~1 request/second limit even when refreshNow() is spammed.
                val sinceLast = SystemClock.elapsedRealtime() - lastRequestElapsed
                if (sinceLast < MIN_GAP_MS) delay(MIN_GAP_MS - sinceLast)
                lastRequestElapsed = SystemClock.elapsedRealtime()
                try {
                    val aircraft = repository.aircraftNear(center, GameRules.QUERY_RADIUS_NM)
                    val planes = Radar.nearby(aircraft, center, tiers.table)
                    val seenAt = clock.now()
                    cacheDao.upsertAll(planes.map { NearbyPlaneCacheEntity.from(it.aircraft, seenAt) })
                    cacheDao.deleteOlderThan(seenAt - CACHE_TTL_MS)
                    _state.update {
                        it.copy(
                            center = center,
                            jumpTarget = activeJump,
                            planes = planes,
                            lastUpdatedMs = seenAt,
                            error = null,
                            hasLocationPermission = location.hasPermission(),
                            mockLocation = activeJump == null && dev?.isMock == true,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: PlaneDataException) {
                    failed = true
                    _state.update { it.copy(center = center, jumpTarget = activeJump, error = e.message) }
                } catch (e: Exception) {
                    failed = true
                    _state.update { it.copy(center = center, jumpTarget = activeJump, error = "Something went wrong") }
                }
            }

            when {
                // Waiting for the first fix: check again soon.
                center == null -> withTimeoutOrNull(WAIT_FOR_FIX_MS) { refreshSignal.receive() }
                // After an error or 429, back off and ignore early refresh requests.
                failed -> delay(PollSchedule.nextDelayMs(lastCallFailed = true))
                else -> withTimeoutOrNull(PollSchedule.nextDelayMs(lastCallFailed = false)) { refreshSignal.receive() }
            }
        }
    }

    private companion object {
        const val MIN_GAP_MS = 2_000L
        const val WAIT_FOR_FIX_MS = 3_000L
        const val CACHE_TTL_MS = 6 * GameRules.HOUR_MS
    }
}
