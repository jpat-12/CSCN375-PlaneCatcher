package com.planecatcher.core

import com.planecatcher.core.model.Aircraft
import com.planecatcher.core.model.GeoPoint
import com.planecatcher.core.model.Tier
import com.planecatcher.core.radar.AlertPolicy
import com.planecatcher.core.radar.PollSchedule
import com.planecatcher.core.radar.Radar
import com.planecatcher.core.tier.TierTable
import org.junit.Assert.assertEquals
import org.junit.Test

class RadarTest {
    private val center = GeoPoint(40.0, -74.0)
    // One degree of latitude is about 69 miles.
    private fun at(hex: String, milesNorth: Double, type: String = "B738", ground: Boolean = false) =
        Aircraft(hex, typeCode = type, lat = 40.0 + milesNorth / 69.05, lon = -74.0, onGround = ground)

    @Test
    fun filters_to_ten_miles_sorted_and_skips_ground() {
        val planes = listOf(at("far", 12.0), at("mid", 6.0), at("near", 1.0), at("gnd", 0.5, ground = true), at("near", 1.0))
        val result = Radar.nearby(planes, center, TierTable.categoryOnly)
        assertEquals(listOf("near", "mid"), result.map { it.hex })
    }

    @Test
    fun alerts_once_per_plane_and_respect_min_tier() {
        val planes = Radar.nearby(
            listOf(at("common", 1.0, "B738"), at("rare", 2.0, "CRJ9"), at("caught", 3.0, "A388"), at("seen", 4.0, "A359")),
            center, TierTable.categoryOnly,
        )
        val alerts = AlertPolicy.planesToAlert(planes, Tier.RARE, caughtHexes = setOf("caught"), alreadyNotified = setOf("seen"))
        assertEquals(listOf("rare"), alerts.map { it.hex })
    }

    @Test
    fun poll_backoff() {
        assertEquals(30_000L, PollSchedule.nextDelayMs(false))
        assertEquals(60_000L, PollSchedule.nextDelayMs(true))
    }
}
