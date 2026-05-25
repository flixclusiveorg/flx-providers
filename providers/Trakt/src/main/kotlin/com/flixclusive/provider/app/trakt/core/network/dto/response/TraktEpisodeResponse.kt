package com.flixclusive.provider.app.trakt.core.network.dto.response

import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.provider.app.trakt.core.model.TraktGenericIdMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
internal data class TraktEpisodeResponse(
    @SerialName("ids") private val ids: TraktGenericIdMap,
    @SerialName("season") val season: Int,
    @SerialName("number") val number: Int,
    @SerialName("first_aired") val firstAired: String? = null,
    @SerialName("images") private val images: Map<String, List<String>>? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("rating") val rating: Double? = null,
    @SerialName("runtime") val runtime: Int? = null,
    @SerialName("title") val title: String? = null
) {
    val id get() = ids.id

    val image get()
        = (images?.get("poster")?.firstOrNull()
                ?: images?.get("thumb")?.firstOrNull()
                ?: images?.get("screenshot")?.firstOrNull())
                ?.let { "https://$it" }

    private fun isReleased(): Boolean {
        if (firstAired == null) return false
        val firstAiredInstant = Instant.parse(firstAired)

        return firstAiredInstant <= Clock.System.now()
    }

    companion object {
        fun TraktEpisodeResponse.toEpisode(): Episode {
            return Episode(
                id = id,
                title = title,
                season = season,
                number = number,
                image = image,
                overview = overview,
                rating = rating,
                releaseDate = firstAired?.let { Instant.parse(it).toEpochMilliseconds() },
                runtime = runtime,
                isReleased = isReleased(),
            )
        }
    }

}