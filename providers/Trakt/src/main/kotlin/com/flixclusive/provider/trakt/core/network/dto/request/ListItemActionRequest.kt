package com.flixclusive.provider.trakt.core.network.dto.request


import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.provider.trakt.core.model.TraktGenericIdMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ListItemActionRequest(
    @SerialName("movies")
    val movies: List<TraktMovie>? = null,
    @SerialName("shows")
    val shows: List<TraktShows>? = null
) {
    companion object {
        fun MediaMetadata.toListItemActionRequest(): ListItemActionRequest {
            val ids = TraktGenericIdMap(
                slug = id,
                tmdb = externalIds[MediaIdSource.TMDB]?.toIntOrNull(),
                imdb = externalIds[MediaIdSource.IMDB],
                trakt = externalIds[MediaIdSource.TRAKT]?.toIntOrNull(),
                tvdb = externalIds[MediaIdSource.TVDB]?.toIntOrNull(),
            )

            return if (type == MediaType.MOVIE) {
                ListItemActionRequest(
                    movies = listOf(TraktMovie(ids = ids)),
                )
            } else {
                ListItemActionRequest(
                    shows = listOf(TraktShows(ids = ids)),
                )
            }
        }

    }

    @Serializable
    data class TraktMovie(
        @SerialName("ids")
        private val ids: TraktGenericIdMap
    )

    @Serializable
    data class TraktShows(
        @SerialName("ids")
        private val ids: TraktGenericIdMap
    )
}