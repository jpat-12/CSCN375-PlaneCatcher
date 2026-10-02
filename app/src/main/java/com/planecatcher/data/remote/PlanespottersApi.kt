package com.planecatcher.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

/** planespotters.net public photo API. Photographer credit must be shown with every photo. */
interface PlanespottersApi {
    @GET("pub/photos/hex/{hex}")
    suspend fun photosByHex(@Path("hex") hex: String): PhotosResponse

    companion object {
        const val BASE_URL = "https://api.planespotters.net/"
    }
}

@Serializable
data class PhotosResponse(val photos: List<PhotoDto> = emptyList())

@Serializable
data class PhotoDto(
    val id: String? = null,
    val thumbnail: ThumbDto? = null,
    @SerialName("thumbnail_large") val thumbnailLarge: ThumbDto? = null,
    val link: String? = null,
    val photographer: String? = null,
)

@Serializable
data class ThumbDto(val src: String? = null)
