package com.flxProviders.sudoflix.api.ridomovies

import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.SearchResponseData
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.provider.ProviderApi
import com.flxProviders.sudoflix.api.ridomovies.RidoMoviesConstant.RIDO_MOVIES_BASE_URL
import com.flxProviders.sudoflix.api.ridomovies.dto.RidoMoviesEmbedDto
import com.flxProviders.sudoflix.api.ridomovies.dto.RidoMoviesSearchDto
import com.flxProviders.sudoflix.api.ridomovies.extractor.CloseLoad
import com.flxProviders.sudoflix.api.ridomovies.extractor.Ridoo
import okhttp3.OkHttpClient
import org.jsoup.Jsoup

class RidoMoviesApi(
    client: OkHttpClient
) : ProviderApi(client) {
    override val name: String
        get() = "RidoMovies"

    override val baseUrl: String
        get() = RIDO_MOVIES_BASE_URL

    private val closeLoad = CloseLoad(client)
    private val ridoo = Ridoo(client)

    override suspend fun getSourceLinks(
        watchId: String,
        film: FilmDetails,
        episode: Episode?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
        val fullSlug = getFullSlug(
            filmTitle = film.title,
            tmdbId = film.tmdbId!!
        )

        val episodeId: String
        val iframeSourceUrl = if (film.filmType == FilmType.TV_SHOW) {
            episodeId = getEpisodeId(
                fullSlug = fullSlug,
                season = episode!!.season,
                episode = episode.number
            )

            "$baseUrl/core/api/episodes/$episodeId/videos"
        } else "$baseUrl/core/api/$fullSlug/videos"

        val embedUrl = getEmbedUrl(urlQuery = iframeSourceUrl)

        when {
            embedUrl.contains("closeload") -> {
                closeLoad.extract(
                    url = embedUrl,
                    onLinkLoaded = onLinkLoaded,
                    onSubtitleLoaded = onSubtitleLoaded
                )
            }
            embedUrl.contains("ridoo") -> {
                ridoo.extract(
                    url = embedUrl,
                    onLinkLoaded = onLinkLoaded,
                    onSubtitleLoaded = onSubtitleLoaded
                )
            }
        }
    }

    private fun getFullSlug(
        filmTitle: String,
        tmdbId: Int,
    ): String {
        val filmTitleQuery = filmTitle.replace(" ", "+")
        val initialResponse = client.request(
            url = "$baseUrl/core/api/search?q=$filmTitleQuery"
        ).execute()
            .use {
                val stringResponse = it.body?.string()

                if (!it.isSuccessful || stringResponse == null) {
                    throw IllegalStateException("[$name]> Could not get full slug")
                }

                fromJson<RidoMoviesSearchDto>(stringResponse)
            }

        val noSearchResultsException = IllegalStateException("[$name]> Could not find full slug")
        if (initialResponse.data.items.isEmpty()) {
            throw noSearchResultsException
        }

        initialResponse.data.items.forEach {
            if (it.contentTable.tmdbId == tmdbId) {
                return it.fullSlug
            }
        }

        throw noSearchResultsException
    }

    private fun getEpisodeId(
        fullSlug: String,
        season: Int,
        episode: Int,
    ): String {
        val fullEpisodeSlug = "season-$season/episode-$episode"

        val regexPattern = Regex(
            """
                \\"id\\":\\"(\d+)\\"(?=.*?\\"fullSlug\\":\\"[^"]*$fullEpisodeSlug[^"]*\\")
            """.trimIndent()
        )

        val episodeIds = client.request(
            url = "$baseUrl/$fullSlug"
        ).execute()
            .use {
                val responseStr = it.body?.string()

                if (!it.isSuccessful || responseStr == null) {
                    throw IllegalStateException("[$name]> Could not get episode ids")
                }

                regexPattern.findAll(responseStr)
                    .map { match ->
                        match.groupValues[1]
                    }.toList()
            }

        val noEpisodeIdFoundException = IllegalStateException("[$name]> Could not find the episode id")
        if (episodeIds.isEmpty()) {
            throw noEpisodeIdFoundException
        }

        val episodeId = episodeIds.lastOrNull()
            ?: throw noEpisodeIdFoundException

        return episodeId
    }

    private fun getEmbedUrl(
        urlQuery: String
    ): String {
        return client.request(
            url = urlQuery
        ).execute()
            .use {
                val stringResponse = it.body?.string()

                val noEmbedDetailsException = IllegalStateException("[$name]> Could not get embed details")
                if (!it.isSuccessful || stringResponse == null) {
                    throw noEmbedDetailsException
                }

                val iFrame = fromJson<RidoMoviesEmbedDto>(stringResponse)
                    .data.getOrNull(0)
                    ?.get("url")
                    ?: throw noEmbedDetailsException

                Jsoup.parse(iFrame).select("iframe").attr("data-src")
            }
    }
}