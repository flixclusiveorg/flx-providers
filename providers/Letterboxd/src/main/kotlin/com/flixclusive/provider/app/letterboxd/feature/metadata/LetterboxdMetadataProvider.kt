package com.flixclusive.provider.app.letterboxd.feature.metadata

import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.provider.app.letterboxd.core.config.LetterboxdConfig
import com.flixclusive.provider.app.letterboxd.core.model.dto.FilmDto
import com.flixclusive.provider.app.letterboxd.core.model.dto.toMovie
import com.flixclusive.provider.app.letterboxd.core.network.LetterboxdApi
import com.flixclusive.provider.capability.MediaMetadataProviderApi

internal class LetterboxdMetadataProvider(
    private val api: LetterboxdApi,
    private val providerId: String,
) : MediaMetadataProviderApi {
    override suspend fun getMovie(media: PartialMedia): Movie = fetchFilm(media.id)

    /** Letterboxd has no television data, so there is nothing to resolve. */
    override suspend fun getShow(media: PartialMedia): Show =
        throw UnsupportedOperationException("Letterboxd covers films only — it has no TV shows.")

    override suspend fun getSeason(
        show: Show,
        season: Season.Partial,
    ): Season.Full? = null

    internal suspend fun fetchFilm(id: String): Movie {
        val body = api.get(path = "${LetterboxdConfig.FILM_PATH}/$id")
        val dto = fromJson<FilmDto>(body)

        if (dto.id.isBlank()) {
            throw IllegalStateException("Letterboxd returned no film for id $id")
        }

        return dto.toMovie(providerId)
    }
}
