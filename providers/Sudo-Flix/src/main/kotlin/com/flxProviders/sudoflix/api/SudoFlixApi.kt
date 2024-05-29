package com.flxProviders.sudoflix.api

import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.Movie
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.dto.FilmInfo
import com.flixclusive.provider.dto.SearchResultItem
import com.flixclusive.provider.dto.SearchResults
import com.flixclusive.provider.util.TvShowCacheData
import com.flxProviders.sudoflix.api.nsbx.NsbxApi
import okhttp3.OkHttpClient

/**
 *
 * SuperStream = SS
 *
 * Based from [this](https://codeberg.org/cloudstream/cloudstream-extensions/src/branch/master/SuperStream/src/main/kotlin/com/lagradost/SuperStream.kt)
 *
 * */
class SudoFlixApi(
    client: OkHttpClient
) : ProviderApi(client) {
    /**
     * The name of the provider.
     */
    override val name: String
        get() = "Sudo-Flix"

    private val providersList = listOf<ProviderApi>(
        NsbxApi(client)
    )

    /**
     * Retrieves detailed information about a film.
     * @param filmId The ID of the film.
     * @param filmType The type of film.
     * @return a [FilmInfo] instance containing the film's information.
     */
    override suspend fun getFilmInfo(
        filmId: String,
        filmType: FilmType
    ): FilmInfo = throw IllegalAccessException("Not necessary to be called on this provider")

    /**
     * Obtains source links for the provided film, season, and episode.
     * @param filmId The ID of the film. The ID must come from the [search] method.
     * @param film The [Film] object of the film. It could either be a [Movie] or [TvShow].
     * @param episode The episode number. Defaults to null if the film is a movie.
     * @param onLinkLoaded A callback function invoked when a [SourceLink] is loaded.
     * @param onSubtitleLoaded A callback function invoked when a [Subtitle] is loaded.
     */
    override suspend fun getSourceLinks(
        filmId: String,
        film: Film,
        season: Int?,
        episode: Int?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
        providersList.mapAsync {
            it.getSourceLinks(
                filmId = filmId,
                film = film,
                season = season,
                episode = episode,
                onLinkLoaded = onLinkLoaded,
                onSubtitleLoaded = onSubtitleLoaded
            )
        }
    }

    /**
     * Performs a search for films based on the provided query.
     * @param film The [Film] object of the film. It could either be a [Movie] or [TvShow].
     * @param page The page number for paginated results. Defaults to 1.
     * @return a [SearchResults] instance containing the search results.
     */
    override suspend fun search(
        film: Film,
        page: Int,
    ): SearchResults {
        return SearchResults(
            currentPage = 1,
            results = listOf(SearchResultItem(id = film.id.toString(), tmdbId = film.id)),
            hasNextPage = false
        )
    }
}