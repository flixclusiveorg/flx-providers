package com.flxProviders.sudoflix.api

import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.film.filter.FilterList
import com.flixclusive.model.provider.MediaLink
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.SearchResponseData
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.provider.ProviderApi
import com.flxProviders.sudoflix.api.nsbx.NsbxApi
import com.flxProviders.sudoflix.api.nsbx.VidBingeApi
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
    override val name = "Sudo-Flix"

    private val providersList = listOf(
        NsbxApi(client),
        VidBingeApi(client),
        RidoMoviesApi(client),
        PrimeWireApi(client),
        VidSrcToApi(client),
    )

    override suspend fun getLinks(
        watchId: String,
        film: FilmDetails,
        episode: Episode?
    ): List<MediaLink> {
        val links = mutableListOf<MediaLink>()
        providersList.mapAsync {
            val extractedLinks = safeCall {
                it.getLinks(
                    watchId = watchId,
                    film = film,
                    episode = episode
                )
            } ?: return@mapAsync

            links.addAll(extractedLinks)
        }

        return links
    }

    override suspend fun search(
        title: String,
        page: Int,
        id: String?,
        imdbId: String?,
        tmdbId: Int?,
        filters: FilterList,
    ): SearchResponseData<FilmSearchItem> {
        val identifier = id ?: tmdbId?.toString() ?: imdbId
        if (identifier == null) {
            throw IllegalStateException("$name is not a searchable provider. It is a set of providers combined into one.")
        }

        return SearchResponseData(
            results = listOf(
                FilmSearchItem(
                    id = identifier,
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