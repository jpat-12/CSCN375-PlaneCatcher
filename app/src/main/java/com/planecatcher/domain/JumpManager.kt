package com.planecatcher.domain

import com.planecatcher.core.rules.JumpStatus
import com.planecatcher.core.rules.JumpTarget
import com.planecatcher.core.rules.LocationJump
import com.planecatcher.data.prefs.UserPrefs
import com.planecatcher.data.time.TrustedClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Location Jump: once per 24 h, up to 3 h, ends on the first catch. Not a VPN. */
@Singleton
class JumpManager @Inject constructor(
    private val prefs: UserPrefs,
    private val clock: TrustedClock,
    private val tracker: RadarTracker,
) {
    private val mutex = Mutex()

    /** Current status, re-evaluated every second for countdowns. */
    val status: Flow<JumpStatus> = combine(prefs.jumpState, ticker()) { state, _ ->
        LocationJump.status(state, clock.now())
    }.distinctUntilChanged()

    suspend fun activate(target: JumpTarget): Result<Unit> = mutex.withLock {
        val result = LocationJump.activate(prefs.jumpState.first(), target, clock.now())
        result.onSuccess {
            prefs.setJumpState(it)
            tracker.refreshNow()
        }.map { }
    }

    suspend fun endEarly() = mutex.withLock {
        prefs.setJumpState(LocationJump.end(prefs.jumpState.first(), clock.now()))
        tracker.refreshNow()
    }

    /** Called after a successful catch; ends an active jump and starts the cooldown. */
    suspend fun onPlaneCaught() = mutex.withLock {
        val before = prefs.jumpState.first()
        val after = LocationJump.onPlaneCaught(before, clock.now())
        if (after != before) {
            prefs.setJumpState(after)
            tracker.refreshNow()
        }
    }
}
