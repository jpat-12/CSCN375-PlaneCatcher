package com.planecatcher.data

import com.planecatcher.core.model.Aircraft
import com.planecatcher.core.model.GeoPoint
import com.planecatcher.data.remote.AirplanesLiveApi
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

@Singleton
class AirplanesLiveRepository @Inject constructor(
    private val api: AirplanesLiveApi,
) : PlaneRepository {
    override suspend fun aircraftNear(center: GeoPoint, radiusNm: Int): List<Aircraft> = try {
        api.point(round4(center.lat), round4(center.lon), radiusNm).ac.mapNotNull { it.toAircraft() }
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
