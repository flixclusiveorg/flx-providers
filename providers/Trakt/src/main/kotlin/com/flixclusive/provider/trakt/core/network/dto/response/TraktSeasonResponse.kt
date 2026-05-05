package com.flixclusive.provider.trakt.core.network.dto.response

import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.provider.trakt.core.model.TraktGenericIdMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
internal data class TraktSeasonResponse(
    @SerialName("ids") private val ids: TraktGenericIdMap,
    @SerialName("number") val number: Int,
    @SerialName("title") val title: String?,
    @SerialName("rating") val rating: Double?,
    @SerialName("episode_count") val episodeCount: Int?,
    @SerialName("images") private val images: Map<String, List<String>>?,
    @SerialName("first_aired") val firstAired: String?,
    @SerialName("overview") val overview: String?,
) {
    val id get() = ids.id

    val image get()
        = (images?.get("poster")?.firstOrNull()
            ?: images?.get("thumb")?.firstOrNull())
            ?.let { "https://$it" }

    companion object {
        fun TraktSeasonResponse.toSeason(
            episodes: List<Episode>
        ) = Season(
            id = id,
            title = title,
            number = number,
            image = image,
            releaseDate = firstAired?.let { Instant.parse(it).toEpochMilliseconds() },
            rating = rating,
            overview = overview,
            isReleased = firstAired != null,
            episodeCount = episodeCount ?: episodes.size,
            episodes = episodes,
        )
    }

}