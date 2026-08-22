package com.flixclusive.provider.app.letterboxd.core.model.dto

import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.Cast
import com.flixclusive.model.media.common.Genre
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.provider.app.letterboxd.core.model.Member
import java.time.LocalDate
import java.time.ZoneOffset

private const val POSTER_TARGET_WIDTH = 600
private const val BACKDROP_TARGET_WIDTH = 1280
private const val AVATAR_TARGET_WIDTH = 200

/**
 * Letterboxd returns a list of concrete renditions rather than a templated URL, so
 * the closest width wins instead of a size token.
 */
internal fun ImageDto?.bestUrl(targetWidth: Int): String? {
    val sizes = this?.sizes?.filter { it.url.isNotBlank() }
    if (sizes.isNullOrEmpty()) return null

    return sizes.minByOrNull { kotlin.math.abs(it.width - targetWidth) }?.url
}

internal fun FilmDto.posterUrl(): String? {
    val image = if (adult && adultPoster != null) adultPoster else poster
    return image.bestUrl(POSTER_TARGET_WIDTH)
}

internal fun FilmDto.backdropUrl(): String? = backdrop.bestUrl(BACKDROP_TARGET_WIDTH)

/**
 * `Film.links` is deprecated upstream with no documented replacement, so treat a
 * missing entry as normal rather than as a parse failure.
 */
internal fun FilmDto.externalIds(): Map<MediaIdSource, String> =
    buildMap {
        links.forEach { link ->
            if (link.id.isBlank()) return@forEach

            when (link.type.lowercase()) {
                "tmdb" -> put(MediaIdSource.TMDB, link.id)
                "imdb" -> put(MediaIdSource.IMDB, link.id)
            }
        }
    }

internal fun FilmDto.releaseDateMs(): Long? {
    releaseDate
        ?.takeIf { it.isNotBlank() }
        ?.let { raw ->
            runCatching { LocalDate.parse(raw) }
                .getOrNull()
                ?.let { return it.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        }

    return releaseYear?.let {
        LocalDate.of(it, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
}

internal fun FilmDto.letterboxdUrl(): String? =
    links.firstOrNull { it.type.equals("letterboxd", ignoreCase = true) }?.url

private fun FilmDto.toGenres(): List<Genre> =
    genres.filter { it.name.isNotBlank() }.map { Genre(name = it.name) }

private fun FilmDto.toDirectorCasts(): List<Cast> =
    directors
        .filter { it.name.isNotBlank() }
        .map { Cast(name = it.name, character = "Director") }

internal fun FilmDto.toPartialMedia(providerId: String): PartialMedia? {
    if (id.isBlank() || name.isBlank()) return null

    return PartialMedia(
        type = MediaType.MOVIE,
        id = id,
        title = name,
        providerId = providerId,
        posterImage = posterUrl(),
        backdropImage = backdropUrl(),
        adult = adult,
        externalIds = externalIds(),
        genres = toGenres(),
        releaseDate = releaseDateMs(),
        rating = rating,
        language = primaryLanguage?.code,
        overview = description,
        homePage = letterboxdUrl(),
    )
}

internal fun FilmDto.toMovie(providerId: String): Movie =
    Movie(
        id = id,
        title = name,
        providerId = providerId,
        posterImage = posterUrl(),
        backdropImage = backdropUrl(),
        adult = adult,
        externalIds = externalIds(),
        genres = toGenres(),
        casts = toDirectorCasts(),
        releaseDate = releaseDateMs(),
        rating = rating,
        runtime = runTime,
        language = primaryLanguage?.code,
        overview = description,
        tagLine = tagline,
        homePage = letterboxdUrl(),
        customProperties =
            buildMap {
                trailer?.url?.takeIf { it.isNotBlank() }?.let { put("trailer", it) }
                originalName?.takeIf { it.isNotBlank() }?.let { put("originalName", it) }
            },
    )

internal fun MemberSummaryDto.toMember(): Member =
    Member(
        id = id,
        username = username,
        displayName = displayName.ifBlank { username },
        avatarUrl = avatar.bestUrl(AVATAR_TARGET_WIDTH),
    )
