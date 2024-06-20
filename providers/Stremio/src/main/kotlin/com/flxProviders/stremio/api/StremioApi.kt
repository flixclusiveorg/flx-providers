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
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.SearchResponseData
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.settings.ProviderSettingsManager
import com.flxProviders.stremio.api.dto.StreamDto.Companion.toSourceLink
import com.flxProviders.stremio.api.dto.StreamResponse
import com.flxProviders.stremio.api.util.OpenSubtitlesUtil.fetchSubtitles
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
        watchId: String,
        film: FilmDetails,
        season: Int?,
        episode: Int?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
        val imdbId = film.imdbId
            ?: throw NullPointerException("[$name]> Could not get IMDB ID")

        asyncCalls(
            {
                getLinks(
                    imdbId = imdbId,
                    season = season,
                    episode = episode,
                    onLinkLoaded = onLinkLoaded,
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

        addons.mapAsync { addon ->
            val streams = safeCall {
                client.request(
                    url = "${addon.baseUrl}/$slug"
                ).execute().fromJson<StreamResponse>().streams
            } ?: return@mapAsync

            streams.forEach { stream ->
                val sourceLink = stream.toSourceLink()
                if (sourceLink != null) onLinkLoaded(sourceLink)

                stream.subtitles?.forEach sub@ { subtitle ->
                    val isValidUrl = isValidUrl(subtitle.url)
                    if (!isValidUrl) return@sub

                    onSubtitleLoaded(subtitle)
                }
            }
        }
    }

    override suspend fun search(
        title: String,
        page: Int,
        id: String?,
        imdbId: String?,
        tmdbId: Int?
    ): SearchResponseData<FilmSearchItem> {
        // TODO("Add catalog system")

        return SearchResponseData(
            results = listOf(
                FilmSearchItem(
                    id = id ?: tmdbId?.toString() ?: imdbId!!,
                    title = title,
                    providerName = name,
                    posterImage = null,
                    backdropImage = null,
                    homePage = null,
                    filmType = FilmType.MOVIE
                )
            )
        )
    }

}