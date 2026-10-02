package com.planecatcher.data

import com.planecatcher.core.model.Aircraft
import com.planecatcher.core.model.GeoPoint
import com.planecatcher.data.remote.AdsbFiApi
import com.planecatcher.data.remote.AdsbLolApi
import com.planecatcher.data.remote.PointResponse
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Source of live aircraft. Behind an interface so the data provider can be swapped later. */
interface PlaneRepository {
    /** @throws PlaneDataException when the source can't be reached or refuses the request. */
    suspend fun aircraftNear(center: GeoPoint, radiusNm: Int): List<Aircraft>
}

class PlaneDataException(message: String, val rateLimited: Boolean = false, cause: Throwable? = null) :
    Exception(message, cause)

/** Asks adsb.fi first and falls back to adsb.lol if it fails. */
@Singleton
class AdsbRepository @Inject constructor(
    private val adsbFi: AdsbFiApi,
    private val adsbLol: AdsbLolApi,
) : PlaneRepository {
    override suspend fun aircraftNear(center: GeoPoint, radiusNm: Int): List<Aircraft> {
        val lat = round4(center.lat)
        val lon = round4(center.lon)
        val response = try {
            fetch { adsbFi.point(lat, lon, radiusNm) }
        } catch (primary: PlaneDataException) {
            try {
                fetch { adsbLol.point(lat, lon, radiusNm) }
            } catch (backup: PlaneDataException) {
                throw primary
            }
        }
        return response.ac.mapNotNull { it.toAircraft() }
    }

    private suspend fun fetch(call: suspend () -> PointResponse): PointResponse = try {
        call()
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        if (e.code() == 429) {
            throw PlaneDataException("Plane data is busy, retrying shortly", rateLimited = true, cause = e)
        }
        throw PlaneDataException("Plane data error (HTTP ${e.code()})", cause = e)
    } catch (e: IOException) {
        throw PlaneDataException("No connection to plane data", cause = e)
    } catch (e: kotlinx.serialization.SerializationException) {
        throw PlaneDataException("Unexpected plane data", cause = e)
    }

    // ~11 m precision is plenty and keeps URLs short.
    private fun round4(v: Double) = Math.round(v * 10_000) / 10_000.0
}
