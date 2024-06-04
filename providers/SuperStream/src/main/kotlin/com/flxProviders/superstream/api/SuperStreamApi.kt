package com.flxProviders.superstream.api

import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.Movie
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.dto.FilmInfo
import com.flixclusive.provider.dto.SearchResults
import com.flixclusive.provider.util.TvShowCacheData
import com.flxProviders.superstream.api.dto.SuperStreamMediaDetailResponse
import com.flxProviders.superstream.api.dto.SuperStreamMediaDetailResponse.Companion.toMediaInfo
import com.flxProviders.superstream.api.dto.SuperStreamSearchResponse
import com.flxProviders.superstream.api.dto.SuperStreamSearchResponse.SuperStreamSearchItem.Companion.toSearchResultItem
import com.flxProviders.superstream.api.util.Constants.appIdSecond
import com.flxProviders.superstream.api.util.Constants.APP_VERSION
import com.flxProviders.superstream.api.util.SuperStreamUtil.SSMediaType.Companion.fromFilmType
import com.flxProviders.superstream.api.util.SuperStreamUtil.getExpiryDate
import com.flxProviders.superstream.api.util.SuperStreamUtil.raiseOnError
import com.flxProviders.superstream.api.util.superStreamCall
import okhttp3.OkHttpClient

/**
 *
 * SuperStream = SS
 *
 * Based from [this](https://codeberg.org/cloudstream/cloudstream-extensions/src/branch/master/SuperStream/src/main/kotlin/com/lagradost/SuperStream.kt)
 *
 * */
class SuperStreamApi(
    client: OkHttpClient
) : ProviderApi(client) {
    /**
     * The name of the provider.
     */
    override val name: String
        get() = "SuperStream"

    private var tvCacheData = TvShowCacheData()

    /**
     * Retrieves detailed information about a film.
     * @param filmId The ID of the film.
     * @param filmType The type of film.
     * @return a [FilmInfo] instance containing the film's information.
     */
    override suspend fun getFilmInfo(
        filmId: String,
        filmType: FilmType
    ): FilmInfo {
        if (tvCacheData.filmInfo?.id == filmId)
            return tvCacheData.filmInfo!!

        val apiQuery = if (filmType == FilmType.MOVIE) {
            """{"childmode":"0","uid":"","app_version":"$APP_VERSION","appid":"$appIdSecond","module":"Movie_detail","channel":"Website","mid":"$filmId","lang":"en","expired_date":"${getExpiryDate()}","platform":"android","oss":"","group":""}"""
        } else {
            """{"childmode":"0","uid":"","app_version":"$APP_VERSION","appid":"$appIdSecond","module":"TV_detail_1","display_all":"1","channel":"Website","lang":"en","expired_date":"${getExpiryDate()}","platform":"android","tid":"$filmId"}"""
        }

        val data = client.superStreamCall<SuperStreamMediaDetailResponse>(apiQuery)
        data?.msg?.raiseOnError("Failed to fetch movie info.")


        val result = data!!.toMediaInfo(filmType == FilmType.MOVIE)
        tvCacheData = TvShowCacheData(id = filmId, result)

        return result
    }

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
        var linksLoadedCount = 0
        var error: Throwable? = null

        fun loadLinksWithCounter(sourceLink: SourceLink) {
            linksLoadedCount++
            onLinkLoaded(sourceLink)
        }

        asyncCalls(
            {
                try {
                    client.getSourceLinksFromFourthApi(
                        filmId = filmId,
                        filmType = fromFilmType(filmType = film.filmType),
                        season = season,
                        episode = episode,
                        onSubtitleLoaded = onSubtitleLoaded,
                        onLinkLoaded = {
                            loadLinksWithCounter(it)
                        }
                    )
                } catch (e: Throwable) {
                    error = e
                }
            },
            {
                val tvShowInfo = when {
                    season != null -> getFilmInfo(filmId, film.filmType)
                    else -> null
                }

                client.getSourceLinksFromSecondApi(
                    filmId = filmId,
                    season = season,
                    episode = episode,
                    tvShowInfo = tvShowInfo,
                    onSubtitleLoaded = onSubtitleLoaded,
                    onLinkLoaded = {
                        loadLinksWithCounter(it)
                    }
                )
            },
        )

        if (linksLoadedCount == 0)
            throw error ?: IllegalStateException("[SuperStream]> No links loaded.")
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
        val itemsPerPage = 20
        val apiQuery =
            // Originally 8 pagelimit
            """{"childmode":"0","app_version":"$APP_VERSION","appid":"$appIdSecond","module":"Search5","channel":"Website","page":"$page","lang":"en","type":"all","keyword":"${film.title}","pagelimit":"$itemsPerPage","expired_date":"${getExpiryDate()}","platform":"android"}"""

        val response = client.superStreamCall<SuperStreamSearchResponse>(apiQuery, true)

        val mappedItems = response?.results?.map {
            it.toSearchResultItem()
        } ?: throw NullPointerException("Cannot search on SuperStream")

        return SearchResults(
            currentPage = page,
            results = mappedItems,
            hasNextPage = (page * itemsPerPage) < response.total
        )
    }
}