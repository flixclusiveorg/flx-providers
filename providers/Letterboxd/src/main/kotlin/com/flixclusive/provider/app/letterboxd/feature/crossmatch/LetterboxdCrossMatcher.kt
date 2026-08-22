package com.flixclusive.provider.app.letterboxd.feature.crossmatch

import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.provider.app.letterboxd.core.network.FilmIdResolver
import com.flixclusive.provider.app.letterboxd.feature.metadata.LetterboxdMetadataProvider
import com.flixclusive.provider.app.letterboxd.feature.search.LetterboxdSearchProvider
import com.flixclusive.provider.capability.CrossMatchProviderApi
import java.time.Instant
import java.time.ZoneOffset

private const val YEAR_TOLERANCE = 1

/**
 * Matching runs *into* Letterboxd rather than reading ids back out: `Film.links` is
 * deprecated upstream with no replacement, while looking a film up by an external id is
 * documented and reliable.
 *
 * TMDB ids resolve straight off the film path. IMDB ids do not — `/film/imdb:tt…` 404s —
 * so those go through [FilmIdResolver] first.
 */
internal class LetterboxdCrossMatcher(
    private val searchApi: LetterboxdSearchProvider,
    private val metadataApi: LetterboxdMetadataProvider,
    private val idResolver: FilmIdResolver,
) : CrossMatchProviderApi {
    override suspend fun getById(
        mediaType: MediaType,
        sourceIds: Map<MediaIdSource, String>,
    ): MediaMetadata? {
        if (mediaType == MediaType.SHOW) return null

        sourceIds[MediaIdSource.TMDB]?.takeIf { it.isNotBlank() }?.let { tmdbId ->
            safeCall { metadataApi.fetchFilm("tmdb:$tmdbId") }?.let { return it }
        }

        sourceIds[MediaIdSource.IMDB]?.takeIf { it.isNotBlank() }?.let { imdbId ->
            val lid = safeCall { idResolver.toLetterboxdId("imdb:$imdbId") }
            if (lid != null) {
                safeCall { metadataApi.fetchFilm(lid) }?.let { return it }
            }
        }

        return null
    }

    override suspend fun getByFuzzy(media: MediaMetadata): MediaMetadata? {
        if (media.type == MediaType.SHOW) return null

        val title = media.title.takeIf { it.isNotBlank() } ?: return null
        val targetYear = media.releaseDate?.toYear()

        val match =
            searchApi.candidates(title).firstOrNull { candidate ->
                if (!candidate.title.equals(title, ignoreCase = true)) return@firstOrNull false

                val candidateYear = candidate.releaseDate?.toYear()
                // A missing year on either side is not evidence of a mismatch.
                targetYear == null || candidateYear == null ||
                    kotlin.math.abs(candidateYear - targetYear) <= YEAR_TOLERANCE
            } ?: return null

        // A weak guess sends the user to the wrong film, so give up rather than approximate.
        return safeCall { metadataApi.fetchFilm(match.id) }
    }
}

private fun Long.toYear(): Int = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).year
