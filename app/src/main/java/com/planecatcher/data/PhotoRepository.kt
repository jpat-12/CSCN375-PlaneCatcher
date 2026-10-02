package com.planecatcher.data

import android.util.LruCache
import com.planecatcher.data.remote.PlanespottersApi
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

data class PlanePhoto(val url: String, val photographer: String?, val link: String?)

/** Looks up aircraft photos by ICAO hex, with an in-memory cache (including "no photo"). */
@Singleton
class PhotoRepository @Inject constructor(private val api: PlanespottersApi) {
    private object None
    private val cache = LruCache<String, Any>(200)

    fun cached(hex: String): PlanePhoto? = cache.get(hex) as? PlanePhoto

    suspend fun photoFor(hex: String): PlanePhoto? {
        when (val hit = cache.get(hex)) {
            is PlanePhoto -> return hit
            None -> return null
        }
        return try {
            val photo = api.photosByHex(hex).photos.firstNotNullOfOrNull { p ->
                val src = p.thumbnailLarge?.src ?: p.thumbnail?.src ?: return@firstNotNullOfOrNull null
                PlanePhoto(src, p.photographer, p.link)
            }
            cache.put(hex, photo ?: None)
            photo
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null // Don't cache failures; a later attempt may succeed.
        }
    }
}
