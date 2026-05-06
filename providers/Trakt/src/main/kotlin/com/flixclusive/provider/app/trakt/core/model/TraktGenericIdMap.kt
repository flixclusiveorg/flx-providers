package com.flixclusive.provider.app.trakt.core.model

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaIdSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TraktGenericIdMap(
    @SerialName("trakt") val trakt: Int? = null,
    @SerialName("slug") val slug: String? = null,
    @SerialName("imdb") val imdb: String? = null,
    @SerialName("tmdb") val tmdb: Int? = null,
    @SerialName("tvdb") val tvdb: Int? = null
) {
    init {
        require(listOfNotNull(trakt, slug, imdb, tmdb, tvdb).isNotEmpty()) {
            "Exactly one identifier must be provided for a show."
        }
    }

    val id get() = slug
        ?: trakt?.toString()
        ?: imdb
        ?: tmdb?.toString()
        ?: tvdb?.toString()
        ?: throw IllegalStateException("No valid identifier provided.")

    fun asMap(): Map<MediaIdSource, String> {
        return buildMap {
            trakt?.let { put(MediaIdSource.TRAKT, it.toString()) }
            imdb?.let { put(MediaIdSource.IMDB, it) }
            tmdb?.let { put(MediaIdSource.TMDB, it.toString()) }
            tvdb?.let { put(MediaIdSource.TVDB, it.toString()) }
        }
    }

    companion object {
        fun MediaMetadata.toTraktIds(): TraktGenericIdMap {
            return TraktGenericIdMap(
                slug = id,
                trakt = externalIds[MediaIdSource.TRAKT]?.toIntOrNull(),
                imdb = externalIds[MediaIdSource.IMDB],
                tmdb = externalIds[MediaIdSource.TMDB]?.toIntOrNull(),
                tvdb = externalIds[MediaIdSource.TVDB]?.toIntOrNull()
            )
        }
    }
}