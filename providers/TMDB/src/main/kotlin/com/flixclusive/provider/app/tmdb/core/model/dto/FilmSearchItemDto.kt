package com.flixclusive.provider.app.tmdb.core.model.dto

import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.provider.app.tmdb.core.config.ImageConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneOffset

@Serializable
internal data class FilmSearchItemDto(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val overview: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val adult: Boolean = false,
    @SerialName("original_language") val originalLanguage: String? = null,
) {
    fun toPartialMedia(
        providerId: String,
        mediaTypeHint: String? = null,
        imgCfg: ImageConfig = ImageConfig(),
    ): PartialMedia? {
        val type = resolveMediaType(mediaTypeHint) ?: return null
        return PartialMedia(
            id = id.toString(),
            providerId = providerId,
            title = title ?: name ?: return null,
            type = type,
            posterImage = posterPath?.let { "${imgCfg.posterPartialBase}$it" },
            backdropImage = backdropPath?.let { "${imgCfg.backdropPartialBase}$it" },
            overview = overview,
            rating = voteAverage,
            adult = adult,
            language = originalLanguage,
            releaseDate = (releaseDate ?: firstAirDate).parseToEpochMillis(),
            externalIds = mapOf(MediaIdSource.TMDB to id.toString()),
        )
    }

    private fun resolveMediaType(hint: String?): MediaType? {
        val resolved = mediaType ?: hint
        return when {
            resolved == "movie" -> MediaType.MOVIE
            resolved == "tv" -> MediaType.SHOW
            title != null && name == null -> MediaType.MOVIE
            name != null && title == null -> MediaType.SHOW
            else -> null
        }
    }
}

internal fun String?.parseToEpochMillis(): Long? {
    if (isNullOrBlank()) return null
    return try {
        LocalDate.parse(this)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    } catch (_: Throwable) {
        null
    }
}
