package com.planecatcher.core.geo

import com.planecatcher.core.model.GeoPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    const val EARTH_RADIUS_MILES = 3958.7613
    const val MILES_PER_NAUTICAL_MILE = 1.150779
    const val KM_PER_MILE = 1.609344

    /** Great-circle distance between two points, in statute miles. */
    fun distanceMiles(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * sin(dLon / 2).let { it * it }
        return 2 * EARTH_RADIUS_MILES * asin(min(1.0, sqrt(h)))
    }

    /** Initial bearing from [from] to [to], in degrees clockwise from north (0..360). */
    fun bearingDeg(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = Math.toRadians(from.lat)
        val lat2 = Math.toRadians(to.lat)
        val dLon = Math.toRadians(to.lon - from.lon)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(kotlin.math.atan2(y, x)) + 360.0) % 360.0
    }

    fun milesToNauticalMiles(miles: Double): Double = miles / MILES_PER_NAUTICAL_MILE
    fun milesToKm(miles: Double): Double = miles * KM_PER_MILE
}
