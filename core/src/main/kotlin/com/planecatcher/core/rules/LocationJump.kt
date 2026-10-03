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
        JumpTarget("ATL", "Atlanta Hartsfield-Jackson, USA", GeoPoint(33.6407, -84.4277)),
        JumpTarget("DFW", "Dallas/Fort Worth, USA", GeoPoint(32.8998, -97.0403)),
        JumpTarget("DEN", "Denver, USA", GeoPoint(39.8561, -104.6737)),
        JumpTarget("ORD", "Chicago O'Hare, USA", GeoPoint(41.9742, -87.9073)),
        JumpTarget("LAX", "Los Angeles, USA", GeoPoint(33.9416, -118.4085)),
        JumpTarget("JFK", "New York JFK, USA", GeoPoint(40.6413, -73.7781)),
        JumpTarget("EWR", "Newark, USA", GeoPoint(40.6895, -74.1745)),
        JumpTarget("LAS", "Las Vegas, USA", GeoPoint(36.084, -115.1537)),
        JumpTarget("MCO", "Orlando, USA", GeoPoint(28.4312, -81.3081)),
        JumpTarget("MIA", "Miami, USA", GeoPoint(25.7959, -80.287)),
        JumpTarget("CLT", "Charlotte, USA", GeoPoint(35.2144, -80.9473)),
        JumpTarget("SEA", "Seattle-Tacoma, USA", GeoPoint(47.4502, -122.3088)),
        JumpTarget("SFO", "San Francisco, USA", GeoPoint(37.6213, -122.379)),
        JumpTarget("PHX", "Phoenix Sky Harbor, USA", GeoPoint(33.4342, -112.0116)),
        JumpTarget("IAH", "Houston Bush, USA", GeoPoint(29.9902, -95.3368)),
        JumpTarget("BOS", "Boston Logan, USA", GeoPoint(42.3656, -71.0096)),
        JumpTarget("MSP", "Minneapolis-St Paul, USA", GeoPoint(44.8848, -93.2223)),
        JumpTarget("DTW", "Detroit, USA", GeoPoint(42.2162, -83.3554)),
        JumpTarget("PHL", "Philadelphia, USA", GeoPoint(39.8744, -75.2424)),
        JumpTarget("IAD", "Washington Dulles, USA", GeoPoint(38.9531, -77.4565)),
        JumpTarget("SDF", "Louisville (UPS hub), USA", GeoPoint(38.1744, -85.736)),
        JumpTarget("MEM", "Memphis (FedEx hub), USA", GeoPoint(35.0424, -89.9767)),
        JumpTarget("ANC", "Anchorage (cargo hub), USA", GeoPoint(61.1743, -149.9963)),
        JumpTarget("HNL", "Honolulu, USA", GeoPoint(21.3187, -157.9225)),
        JumpTarget("YYZ", "Toronto Pearson, Canada", GeoPoint(43.6777, -79.6248)),
        JumpTarget("YVR", "Vancouver, Canada", GeoPoint(49.1967, -123.1815)),
        JumpTarget("MEX", "Mexico City, Mexico", GeoPoint(19.4361, -99.0719)),
        JumpTarget("CUN", "Cancún, Mexico", GeoPoint(21.0365, -86.8771)),
        JumpTarget("GRU", "São Paulo Guarulhos, Brazil", GeoPoint(-23.4356, -46.4731)),
        JumpTarget("BOG", "Bogotá, Colombia", GeoPoint(4.7016, -74.1469)),
        JumpTarget("LHR", "London Heathrow, UK", GeoPoint(51.47, -0.4543)),
        JumpTarget("LGW", "London Gatwick, UK", GeoPoint(51.1537, -0.1821)),
        JumpTarget("CDG", "Paris Charles de Gaulle, France", GeoPoint(49.0097, 2.5479)),
        JumpTarget("FRA", "Frankfurt, Germany", GeoPoint(50.0379, 8.5622)),
        JumpTarget("MUC", "Munich, Germany", GeoPoint(48.3537, 11.775)),
        JumpTarget("AMS", "Amsterdam Schiphol, Netherlands", GeoPoint(52.3105, 4.7683)),
        JumpTarget("MAD", "Madrid Barajas, Spain", GeoPoint(40.4983, -3.5676)),
        JumpTarget("BCN", "Barcelona, Spain", GeoPoint(41.2974, 2.0833)),
        JumpTarget("FCO", "Rome Fiumicino, Italy", GeoPoint(41.8003, 12.2389)),
        JumpTarget("ZRH", "Zurich, Switzerland", GeoPoint(47.4582, 8.5555)),
        JumpTarget("IST", "Istanbul, Turkey", GeoPoint(41.2753, 28.7519)),
        JumpTarget("TLS", "Toulouse (Airbus home), France", GeoPoint(43.6291, 1.3638)),
        JumpTarget("DXB", "Dubai, UAE", GeoPoint(25.2532, 55.3657)),
        JumpTarget("DOH", "Doha Hamad, Qatar", GeoPoint(25.2731, 51.6081)),
        JumpTarget("AUH", "Abu Dhabi, UAE", GeoPoint(24.433, 54.6511)),
        JumpTarget("DEL", "Delhi, India", GeoPoint(28.5562, 77.1)),
        JumpTarget("BOM", "Mumbai, India", GeoPoint(19.0896, 72.8656)),
        JumpTarget("SIN", "Singapore Changi, Singapore", GeoPoint(1.3644, 103.9915)),
        JumpTarget("HKG", "Hong Kong, China", GeoPoint(22.308, 113.9185)),
        JumpTarget("PEK", "Beijing Capital, China", GeoPoint(40.0799, 116.6031)),
        JumpTarget("PVG", "Shanghai Pudong, China", GeoPoint(31.1443, 121.8083)),
        JumpTarget("HND", "Tokyo Haneda, Japan", GeoPoint(35.5494, 139.7798)),
        JumpTarget("NRT", "Tokyo Narita, Japan", GeoPoint(35.772, 140.3929)),
        JumpTarget("ICN", "Seoul Incheon, South Korea", GeoPoint(37.4602, 126.4407)),
        JumpTarget("TPE", "Taipei Taoyuan, Taiwan", GeoPoint(25.0797, 121.2342)),
        JumpTarget("BKK", "Bangkok Suvarnabhumi, Thailand", GeoPoint(13.69, 100.7501)),
        JumpTarget("KUL", "Kuala Lumpur, Malaysia", GeoPoint(2.7456, 101.7072)),
        JumpTarget("SYD", "Sydney, Australia", GeoPoint(-33.9399, 151.1753)),
        JumpTarget("MEL", "Melbourne, Australia", GeoPoint(-37.669, 144.841)),
        JumpTarget("AKL", "Auckland, New Zealand", GeoPoint(-37.0082, 174.785)),
        JumpTarget("JNB", "Johannesburg, South Africa", GeoPoint(-26.1367, 28.2411)),
        JumpTarget("CAI", "Cairo, Egypt", GeoPoint(30.1219, 31.4056)),
    )

    /** Matches airport code, city or name, ignoring case. Exact code matches come first. */
    fun search(query: String): List<JumpTarget> {
        val q = query.trim()
        if (q.isEmpty()) return all
        return all.filter { it.code.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true) }
            .sortedBy { if (it.code.equals(q, ignoreCase = true)) 0 else 1 }
    }

    /** A custom spot picked on the map. */
    fun custom(point: GeoPoint): JumpTarget = JumpTarget(
        CUSTOM_CODE,
        String.format(java.util.Locale.US, "Map pin (%.2f, %.2f)", point.lat, point.lon),
        point,
    )

    const val CUSTOM_CODE = "PIN"
}
