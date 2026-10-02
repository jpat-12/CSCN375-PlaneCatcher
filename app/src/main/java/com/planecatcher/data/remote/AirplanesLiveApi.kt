package com.planecatcher.data.remote

import com.planecatcher.core.model.Aircraft
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * airplanes.live REST API. Non-commercial use only, about 1 request per second.
 * https://airplanes.live/api-guide/
 */
interface AirplanesLiveApi {
    @GET("v2/point/{lat}/{lon}/{radius}")
    suspend fun point(
        @Path("lat") lat: Double,
        @Path("lon") lon: Double,
        @Path("radius") radiusNm: Int,
    ): PointResponse

    companion object {
        const val BASE_URL = "https://api.airplanes.live/"
    }
}

@Serializable
data class PointResponse(
    val ac: List<AircraftDto> = emptyList(),
    val now: Long? = null,
)

@Serializable
data class AircraftDto(
    val hex: String,
    val flight: String? = null,
    @SerialName("r") val registration: String? = null,
    @SerialName("t") val typeCode: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    /** Barometric altitude in feet, or the string "ground". */
    @SerialName("alt_baro") val altBaro: JsonElement? = null,
    @SerialName("gs") val groundSpeedKt: Double? = null,
    val track: Double? = null,
    /** Bit 0 set means military. */
    val dbFlags: Int? = null,
) {
    fun toAircraft(): Aircraft? {
        val la = lat ?: return null
        val lo = lon ?: return null
        val alt = altBaro as? JsonPrimitive
        val onGround = alt?.isString == true && alt.content.equals("ground", ignoreCase = true)
        return Aircraft(
            hex = hex.trim().lowercase().removePrefix("~"),
            callsign = flight?.trim()?.takeIf { it.isNotEmpty() },
            registration = registration?.trim()?.takeIf { it.isNotEmpty() },
            typeCode = typeCode?.trim()?.uppercase()?.takeIf { it.isNotEmpty() },
            lat = la,
            lon = lo,
            altitudeFt = if (onGround) 0 else alt?.intOrNull,
            onGround = onGround,
            speedKt = groundSpeedKt,
            headingDeg = track,
            isMilitary = (dbFlags ?: 0) and 1 == 1,
        )
    }
}
