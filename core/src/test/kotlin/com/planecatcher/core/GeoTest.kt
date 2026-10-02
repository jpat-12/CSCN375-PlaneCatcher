package com.planecatcher.core

import com.planecatcher.core.geo.Geo
import com.planecatcher.core.model.GeoPoint
import com.planecatcher.core.rules.GameRules
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoTest {
    @Test
    fun distance_between_known_airports() {
        val jfk = GeoPoint(40.6413, -73.7781)
        val lax = GeoPoint(33.9416, -118.4085)
        // Great-circle JFK-LAX is about 2,470 statute miles.
        assertEquals(2470.0, Geo.distanceMiles(jfk, lax), 15.0)
    }

    @Test
    fun distance_to_self_is_zero() {
        val p = GeoPoint(10.0, 20.0)
        assertEquals(0.0, Geo.distanceMiles(p, p), 1e-9)
    }

    @Test
    fun bearing_due_north_and_east() {
        val origin = GeoPoint(0.0, 0.0)
        assertEquals(0.0, Geo.bearingDeg(origin, GeoPoint(1.0, 0.0)), 1e-6)
        assertEquals(90.0, Geo.bearingDeg(origin, GeoPoint(0.0, 1.0)), 1e-6)
    }

    @Test
    fun query_radius_covers_catch_range() {
        assertEquals(9, GameRules.QUERY_RADIUS_NM)
        val nmInMiles = GameRules.QUERY_RADIUS_NM * Geo.MILES_PER_NAUTICAL_MILE
        assert(nmInMiles >= GameRules.CATCH_RANGE_MILES)
    }
}
