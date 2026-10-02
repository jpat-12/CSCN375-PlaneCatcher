package com.planecatcher.core.model

/**
 * One aircraft as reported by the live data source (airplanes.live).
 * Only [hex] and a position are guaranteed; every other field may be missing.
 */
data class Aircraft(
    /** ICAO 24-bit address in lowercase hex, e.g. "a1b2c3". Stable id for the airframe. */
    val hex: String,
    val callsign: String? = null,
    val registration: String? = null,
    /** ICAO aircraft type designator, e.g. "B738", "A388". */
    val typeCode: String? = null,
    val lat: Double,
    val lon: Double,
    val altitudeFt: Int? = null,
    val onGround: Boolean = false,
    val speedKt: Double? = null,
    val headingDeg: Double? = null,
    val isMilitary: Boolean = false,
) {
    val position: GeoPoint get() = GeoPoint(lat, lon)

    /** Best human label for lists: callsign, then registration, then hex. */
    val displayName: String
        get() = callsign?.takeIf { it.isNotBlank() }
            ?: registration?.takeIf { it.isNotBlank() }
            ?: hex.uppercase()
}
