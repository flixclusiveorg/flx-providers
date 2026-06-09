package com.flixclusive.provider.app.trakt.core.network.dto.response

import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.provider.app.trakt.core.model.TraktGenericIdMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
internal data class TraktSeasonResponse(
    @SerialName("ids") private val ids: TraktGenericIdMap,
    @SerialName("number") val number: Int,
    @SerialName("title") val title: String? = null,
    @SerialName("rating") val rating: Double? = null,
    @SerialName("episode_count") val episodeCount: Int? = null,
    @SerialName("images") private val images: Map<String, List<String>>? = null,
    @SerialName("first_aired") val firstAired: String? = null,
    @SerialName("overview") val overview: String? = null,
) {
    val id get() = ids.id

    val image get()
        = (images?.get("poster")?.firstOrNull()
            ?: images?.get("thumb")?.firstOrNull())
            ?.let { "https://$it" }

    companion object {
        fun TraktSeasonResponse.toSeason() = Season.Partial(
            id = id,
            title = title,
            number = number,
            image = image,
            releaseDate = firstAired?.let { Instant.parse(it).toEpochMilliseconds() },
            rating = rating,
            overview = overview,
            isReleased = firstAired != null,
            episodeCount = episodeCount ?: 0,
        )
    }

}