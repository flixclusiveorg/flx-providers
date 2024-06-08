@file:Suppress("SpellCheckingInspection")

package com.flxProviders.stremio.api

import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.Film
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.dto.SearchResultItem
import com.flixclusive.provider.dto.SearchResults
import com.flixclusive.provider.settings.ProviderSettingsManager
import com.flxProviders.stremio.api.dto.StreamDto.Companion.toSourceLink
import com.flxProviders.stremio.api.dto.StreamResponse
import com.flxProviders.stremio.api.dto.TmdbQueryDto
import com.flxProviders.stremio.api.util.OpenSubtitlesUtil.fetchSubtitles
import com.flxProviders.stremio.api.util.getTmdbQuery
import com.flxProviders.stremio.api.util.isValidUrl
import com.flxProviders.stremio.settings.AddonUtil.getAddons
import okhttp3.OkHttpClient

internal const val STREAMIO_ADDONS_KEY = "streamio_addons"

class StremioApi(
    client: OkHttpClient,
    private val settings: ProviderSettingsManager
) : ProviderApi(client) {
    override val name: String
        get() = "Stremio"

    override suspend fun getSourceLinks(
        filmId: String,
        film: Film,
        season: Int?,
        episode: Int?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
        val imdbId = getImdbId(
            filmId = filmId,
            filmType = film.filmType
        )

        var linksLoaded = 0
        asyncCalls(
            {
                getLinks(
                    imdbId = imdbId,
                    season = season,
                    episode = episode,
                    onLinkLoaded = {
                        linksLoaded++
                        onLinkLoaded(it)
                    },
                    onSubtitleLoaded = onSubtitleLoaded
                )
            },
            {
                client.fetchSubtitles(
                    imdbId = imdbId,
                    season = season,
                    episode = episode,
                    onSubtitleLoaded = onSubtitleLoaded
                )
            },
        )

        if (linksLoaded == 0)
            throw Exception("[$name]> No links found")
    }

    private suspend fun getLinks(
        imdbId: String,
        season: Int?,
        episode: Int?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
        val slug = if(season == null) {
            "stream/movie/$imdbId.json"
        } else {
            "stream/series/$imdbId:$season:$episode.json"
        }

        val addons = settings.getAddons()

        addons.mapAsync {
            val streams = safeCall {
                client.request(
                    url = "${it.baseUrl}/$slug"
                ).execute().fromJson<StreamResponse>().streams
            } ?: return@mapAsync

            streams.forEach {
                val sourceLink = it.toSourceLink()
                if (sourceLink != null) onLinkLoaded(sourceLink)

                it.subtitles?.forEach sub@ { subtitle ->
                    val isValidUrl = isValidUrl(subtitle.url)
                    if (!isValidUrl) return@sub

                    onSubtitleLoaded(subtitle)
                }
            }
        }
    }

    override suspend fun search(film: Film, page: Int): SearchResults {
        return SearchResults(
            results = listOf(
                SearchResultItem(
                    id = film.id.toString(),
                    tmdbId = film.id
                )
            )
        )
    }

    private fun getImdbId(
        filmId: String,
        filmType: FilmType
    ): String {
        val tmdbQuery = getTmdbQuery(
            id = filmId,
            filmType = filmType.type
        )

        val tmdbResponse = client.request(tmdbQuery)
            .execute().fromJson<TmdbQueryDto>("[$name]> Could not get TMDB response")

        return tmdbResponse.imdbId
            ?: tmdbResponse.externalIds["imdb_id"] as String
    }
}