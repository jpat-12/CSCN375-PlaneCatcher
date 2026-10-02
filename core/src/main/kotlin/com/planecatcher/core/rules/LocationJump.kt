package com.planecatcher.core.rules

import com.planecatcher.core.model.GeoPoint

/** A place the user can jump to: usually a busy airport. */
data class JumpTarget(val code: String, val name: String, val point: GeoPoint)

/**
 * Persisted Location Jump state. All times are trusted epoch milliseconds
 * (see the app's TrustedClock), never the raw phone clock.
 */
data class JumpState(
    val target: JumpTarget? = null,
    val activeUntilMs: Long? = null,
    /** When the last jump ended. The 24-hour cooldown counts from here. */
    val endedAtMs: Long? = null,
)

sealed interface JumpStatus {
    data object Ready : JumpStatus
    data class Active(val target: JumpTarget, val remainingMs: Long) : JumpStatus
    data class Cooldown(val remainingMs: Long) : JumpStatus
}

/**
 * Location Jump rules:
 *  - Usable once per 24 hours.
 *  - Lasts up to 3 hours.
 *  - Ends as soon as a plane is caught with it, or when the 3 hours run out.
 *  - A wrong answer does not end it.
 *  - The 24-hour cooldown starts when the jump ends, however it ends.
 */
object LocationJump {

    /** Folds an expired jump into an ended one. Call before reading or saving state. */
    fun normalize(state: JumpState, nowMs: Long): JumpState {
        val until = state.activeUntilMs ?: return state
        return if (nowMs >= until) JumpState(endedAtMs = until) else state
    }

    fun status(state: JumpState, nowMs: Long): JumpStatus {
        val s = normalize(state, nowMs)
        val until = s.activeUntilMs
        if (until != null && s.target != null) {
            val remaining = (until - nowMs).coerceIn(0, GameRules.JUMP_DURATION_MS)
            return JumpStatus.Active(s.target, remaining)
        }
        val ended = s.endedAtMs ?: return JumpStatus.Ready
        val readyAt = ended + GameRules.JUMP_COOLDOWN_MS
        // If the clock appears to have gone backwards (now < ended) we stay in cooldown;
        // rewinding the phone clock must never unlock a jump early.
        return if (nowMs < readyAt) JumpStatus.Cooldown(readyAt - nowMs) else JumpStatus.Ready
    }

    /** Starts a jump. Fails if one is active or the cooldown is still running. */
    fun activate(state: JumpState, target: JumpTarget, nowMs: Long): Result<JumpState> =
        when (val st = status(state, nowMs)) {
            JumpStatus.Ready -> Result.success(
                JumpState(target = target, activeUntilMs = nowMs + GameRules.JUMP_DURATION_MS),
            )
            is JumpStatus.Active -> Result.failure(IllegalStateException("A Location Jump is already active"))
            is JumpStatus.Cooldown -> Result.failure(
                IllegalStateException("Location Jump is cooling down for ${st.remainingMs} ms"),
            )
        }

    /** A plane was caught. If a jump was active, it ends now and the cooldown starts. */
    fun onPlaneCaught(state: JumpState, nowMs: Long): JumpState = end(state, nowMs)

    /** The user ended the jump early. Same effect as catching a plane. */
    fun end(state: JumpState, nowMs: Long): JumpState {
        val s = normalize(state, nowMs)
        return if (s.activeUntilMs != null) JumpState(endedAtMs = nowMs) else s
    }

    fun isActive(state: JumpState, nowMs: Long): Boolean = status(state, nowMs) is JumpStatus.Active
}

/** Busy airports that make good jump destinations. */
object JumpDestinations {
    val all: List<JumpTarget> = listOf(
        JumpTarget("ATL", "Atlanta Hartsfield-Jackson", GeoPoint(33.6407, -84.4277)),
        JumpTarget("DFW", "Dallas/Fort Worth", GeoPoint(32.8998, -97.0403)),
        JumpTarget("ORD", "Chicago O'Hare", GeoPoint(41.9742, -87.9073)),
        JumpTarget("LAX", "Los Angeles", GeoPoint(33.9416, -118.4085)),
        JumpTarget("JFK", "New York JFK", GeoPoint(40.6413, -73.7781)),
        JumpTarget("ANC", "Anchorage (cargo hub)", GeoPoint(61.1743, -149.9963)),
        JumpTarget("LHR", "London Heathrow", GeoPoint(51.4700, -0.4543)),
        JumpTarget("CDG", "Paris Charles de Gaulle", GeoPoint(49.0097, 2.5479)),
        JumpTarget("FRA", "Frankfurt", GeoPoint(50.0379, 8.5622)),
        JumpTarget("AMS", "Amsterdam Schiphol", GeoPoint(52.3105, 4.7683)),
        JumpTarget("DXB", "Dubai", GeoPoint(25.2532, 55.3657)),
        JumpTarget("DOH", "Doha Hamad", GeoPoint(25.2731, 51.6081)),
        JumpTarget("SIN", "Singapore Changi", GeoPoint(1.3644, 103.9915)),
        JumpTarget("HND", "Tokyo Haneda", GeoPoint(35.5494, 139.7798)),
        JumpTarget("ICN", "Seoul Incheon", GeoPoint(37.4602, 126.4407)),
        JumpTarget("SYD", "Sydney", GeoPoint(-33.9399, 151.1753)),
    )
}
