package com.planecatcher.core.radar

import com.planecatcher.core.geo.Geo
import com.planecatcher.core.model.Aircraft
import com.planecatcher.core.model.GeoPoint
import com.planecatcher.core.model.Tier
import com.planecatcher.core.rules.GameRules
import com.planecatcher.core.tier.TierTable

/** An aircraft placed relative to the radar centre. */
data class NearbyPlane(
    val aircraft: Aircraft,
    val distanceMiles: Double,
    val bearingDeg: Double,
    val tier: Tier,
) {
    val hex: String get() = aircraft.hex
    val inRange: Boolean get() = distanceMiles <= GameRules.CATCH_RANGE_MILES
}

object Radar {
    /**
     * Places every airborne aircraft relative to [center], keeps the ones within
     * [rangeMiles], and sorts nearest first. Planes on the ground are skipped.
     */
    fun nearby(
        aircraft: List<Aircraft>,
        center: GeoPoint,
        tiers: TierTable,
        rangeMiles: Double = GameRules.CATCH_RANGE_MILES,
    ): List<NearbyPlane> = aircraft
        .asSequence()
        .filter { !it.onGround }
        .distinctBy { it.hex }
        .map {
            NearbyPlane(
                aircraft = it,
                distanceMiles = Geo.distanceMiles(center, it.position),
                bearingDeg = Geo.bearingDeg(center, it.position),
                tier = tiers.classify(it.typeCode, it.isMilitary),
            )
        }
        .filter { it.distanceMiles <= rangeMiles }
        .sortedBy { it.distanceMiles }
        .toList()
}

/** Decides which in-range planes deserve a "plane nearby" alert. */
object AlertPolicy {
    /**
     * Returns the planes to alert about: in range, at least [minTier], not already
     * caught, and not alerted before. Callers add the returned hexes to [alreadyNotified].
     */
    fun planesToAlert(
        planes: List<NearbyPlane>,
        minTier: Tier,
        caughtHexes: Set<String>,
        alreadyNotified: Set<String>,
    ): List<NearbyPlane> = planes.filter {
        it.inRange &&
            it.tier.atLeast(minTier) &&
            it.hex !in caughtHexes &&
            it.hex !in alreadyNotified
    }
}

/** Poll timing: every 30 s normally, 60 s after an error or HTTP 429. */
object PollSchedule {
    fun nextDelayMs(lastCallFailed: Boolean): Long =
        if (lastCallFailed) GameRules.ERROR_BACKOFF_MS else GameRules.POLL_INTERVAL_MS
}
