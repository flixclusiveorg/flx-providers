package com.flxProviders.sudoflix.api

import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.SearchResponseData
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.provider.ProviderApi
import com.flxProviders.sudoflix.api.nsbx.NsbxApi
import com.flxProviders.sudoflix.api.primewire.PrimeWireApi
import com.flxProviders.sudoflix.api.ridomovies.RidoMoviesApi
import com.flxProviders.sudoflix.api.vidsrcto.VidSrcToApi
import okhttp3.OkHttpClient

/**
 *
 * RIP m-w
 * */
class SudoFlixApi(
    client: OkHttpClient
) : ProviderApi(client) {
    override val baseUrl: String
        get() = super.baseUrl

    override val name: String
        get() = "Sudo-Flix"

    private val providersList = listOf(
        NsbxApi(client),
        RidoMoviesApi(client),
        PrimeWireApi(client),
        VidSrcToApi(client),
    )

    override suspend fun getSourceLinks(
        watchId: String,
        film: FilmDetails,
        episode: Episode?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
        providersList.mapAsync {
            safeCall {
                it.getSourceLinks(
                    watchId = watchId,
                    film = film,
                    episode = episode,
                    onLinkLoaded = onLinkLoaded,
                    onSubtitleLoaded = onSubtitleLoaded
                )
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
        return SearchResponseData(
            results = listOf(
                FilmSearchItem(
                    id = id ?: tmdbId?.toString() ?: imdbId!!,
                    title = title,
                    providerName = name,
                    filmType = FilmType.MOVIE,
                    posterImage = null,
                    backdropImage = null,
                    homePage = null
                )
            )
        )
    }
}