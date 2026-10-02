package com.planecatcher.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.planecatcher.core.model.Aircraft
import com.planecatcher.core.model.Tier

@Entity(tableName = "caught_planes")
data class CaughtPlaneEntity(
    /** ICAO hex; one catch per airframe. */
    @PrimaryKey val hex: String,
    val callsign: String?,
    val registration: String?,
    val typeCode: String?,
    val typeName: String?,
    val tier: Tier,
    val points: Int,
    val caughtAt: Long,
    /** Where the catcher was (or jumped to) when the plane was caught. */
    val lat: Double,
    val lon: Double,
    val altitudeFt: Int?,
    val distanceMiles: Double,
    val isMilitary: Boolean,
    /** Airport code of the Location Jump used, if any. */
    val jumpCode: String?,
    val photoUrl: String?,
    val photographer: String?,
    val photoLink: String?,
)

/** Last-seen aircraft, so the quiz and lists work for a while without a fresh fetch. */
@Entity(tableName = "nearby_plane_cache")
data class NearbyPlaneCacheEntity(
    @PrimaryKey val hex: String,
    val callsign: String?,
    val registration: String?,
    val typeCode: String?,
    val lat: Double,
    val lon: Double,
    val altitudeFt: Int?,
    val speedKt: Double?,
    val heading: Double?,
    val isMilitary: Boolean,
    val lastSeen: Long,
) {
    fun toAircraft() = Aircraft(
        hex = hex, callsign = callsign, registration = registration, typeCode = typeCode,
        lat = lat, lon = lon, altitudeFt = altitudeFt, speedKt = speedKt, headingDeg = heading,
        isMilitary = isMilitary,
    )

    companion object {
        fun from(a: Aircraft, seenAt: Long) = NearbyPlaneCacheEntity(
            hex = a.hex, callsign = a.callsign, registration = a.registration, typeCode = a.typeCode,
            lat = a.lat, lon = a.lon, altitudeFt = a.altitudeFt, speedKt = a.speedKt, heading = a.headingDeg,
            isMilitary = a.isMilitary, lastSeen = seenAt,
        )
    }
}

/** A plane whose quiz was failed; locked until [retryAt]. */
@Entity(tableName = "quiz_locks")
data class QuizLockEntity(
    @PrimaryKey val planeHex: String,
    val retryAt: Long,
    val failedKind: String?,
)
