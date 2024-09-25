package com.flxProviders.flixhq.api

import android.content.Context
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.provider.filter.FilterList
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmDetails
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderWebViewApi
import com.flixclusive.provider.webview.ProviderWebView
import com.flxProviders.flixhq.api.util.TvShowCacheData
import com.flxProviders.flixhq.api.util.getEpisodeId
import com.flxProviders.flixhq.api.util.getSeasonId
import com.flxProviders.flixhq.api.util.getServerName
import com.flxProviders.flixhq.api.util.getServerUrl
import com.flxProviders.flixhq.api.util.replaceWhitespaces
import com.flxProviders.flixhq.api.util.toFilmSearchItem
import com.flxProviders.flixhq.extractors.rabbitstream.UpCloud
import com.flxProviders.flixhq.extractors.rabbitstream.VidCloud
import com.flxProviders.flixhq.webview.FlixHQWebView
import com.flxProviders.flixhq.webview.util.removeAccents
import okhttp3.OkHttpClient
import org.jsoup.Jsoup

class FlixHQApi(
    client: OkHttpClient,
    context: Context,
    provider: Provider
) : ProviderWebViewApi(
    client = client,
    context = context,
    provider = provider
) {
    override val baseUrl: String = "https://flixhq.to"
    private var tvCacheData: TvShowCacheData = TvShowCacheData()

    @Suppress("SpellCheckingInspection")
    internal val extractors = mapOf(
        "vidcloud" to VidCloud(client),
        "upcloud" to UpCloud(client),
    )

    override suspend fun search(
        title: String,
        page: Int,
        id: String?,
        imdbId: String?,
        tmdbId: Int?,
        filters: FilterList,
    ): SearchResponseData<FilmSearchItem> {
        val query = title.removeAccents()

        var searchResult = SearchResponseData<FilmSearchItem>(
            page = page,
            hasNextPage = false,
            results = emptyList()
        )

        client.request(
            url = "${baseUrl}/search/${
                query.replaceWhitespaces("-")
            }?page=$page"
        ).execute().body?.string()?.let { data ->
            val doc = Jsoup.parse(data)
            val navSelector = "div.pre-pagination:nth-child(3) > nav:nth-child(1) > ul:nth-child(1)"
            searchResult = searchResult.copy(
                hasNextPage = doc.select(navSelector).size > 0 && doc.select(navSelector).last()
                    ?.hasClass("active") == false
            )

            val results = mutableListOf<FilmSearchItem>()

            doc.select(".film_list-wrap > div.flw-item").forEach { element ->
                results.add(
                    element.toFilmSearchItem(
                        baseUrl = baseUrl,
                        provider = provider.name
                    )
                )
            }

            return searchResult.copy(results = results)
        }

        return SearchResponseData()
    }

    override suspend fun getFilmDetails(film: Film): FilmDetails {
        var watchIdToUse = film.identifier
        val filmType = film.filmType
        
        if (!watchIdToUse.startsWith(baseUrl)) {
            watchIdToUse = "$baseUrl/$watchIdToUse"
        }

        if (tvCacheData.filmInfo?.id == watchIdToUse)
            return tvCacheData.filmInfo!!

        var filmDetails = Movie(
            id = watchIdToUse.split("to/").last(),
            providerName = film.providerName,
            title = film.title,
            posterImage = film.posterImage,
            backdropImage = film.backdropImage,
            homePage = film.homePage,
        )

        val response = client.request(url = watchIdToUse).execute()
        val data = response.body?.string()

        if (data != null) {
            val doc = Jsoup.parse(data)
            filmDetails = filmDetails.copy(
                title = doc.select(".heading-name > a:nth-child(1)").text()
            )

            val uid = doc.select(".watch_block").attr("data-id")
            val releaseDate = Jsoup.parse(data).select("div.row-line:nth-child(3)").text()
                .replace("Released: ", "").trim()
            val year = releaseDate.split("-").firstOrNull()?.toIntOrNull()

            filmDetails = filmDetails.copy(year = year)

            if (filmType == FilmType.MOVIE) {
                filmDetails = filmDetails.copy(
                    id = uid,
                    title = "${filmDetails.title} Movie",
                )
            }

            tvCacheData = TvShowCacheData(id = uid, filmDetails)
            return filmDetails
        }

        throw NullPointerException("FilmInfo is null!")
    }

    override fun getWebView(): ProviderWebView {
        return FlixHQWebView(
            mClient = client,
            context = context,
            api = this
        )
    }

    internal fun getEpisodeId(
        watchId: String,
        episode: Int,
        season: Int,
    ): String {
        val watchIdToUse = watchId.split("-").last()
        val isSameId = tvCacheData.id == watchIdToUse

        val ajaxReqUrl: (String, Boolean) -> String = { id, isSeasons ->
            "$baseUrl/ajax/season/${if (isSeasons) "list" else "episodes"}/$id"
        }

        if (isSameId) {
            tvCacheData.episodes
                ?.getEpisodeId(episode)
                ?.let {
                    return it
                }
        }

        if (tvCacheData.seasons == null || !isSameId) {
            val responseSeasons =
                client.request(url = ajaxReqUrl(watchIdToUse, true)).execute()
            val dataSeasons = responseSeasons.body?.string()
                ?: throw Exception("Failed to fetch season data from provider")

            val seasonsDoc = Jsoup.parse(dataSeasons)
                .select(".dropdown-menu > a")

            tvCacheData = if (isSameId) {
                tvCacheData.copy(seasons = seasonsDoc)
            } else {
                TvShowCacheData(id = watchIdToUse, seasons = seasonsDoc)
            }
        }

        val seasons = tvCacheData.seasons!!

        if (seasons.size < season)
            throw Exception("Season $season is not available.")

        val seasonId =
            seasons.getSeasonId(season) ?: throw Exception("Season $season could not be found!")

        val responseEpisodes = client.request(url = ajaxReqUrl(seasonId, false)).execute()
        val dataEpisodes = responseEpisodes.body?.string()
            ?: throw Exception("Failed to fetch episode id from provider")

        val docEpisodes = Jsoup.parse(dataEpisodes)
        val episodes = docEpisodes.select(".nav > li")

        tvCacheData = if (isSameId) {
            tvCacheData.copy(episodes = episodes)
        } else {
            TvShowCacheData(
                id = watchIdToUse,
                seasons = seasons,
                episodes = episodes
            )
        }

        return episodes.getEpisodeId(episode) ?: throw Exception("Cannot find episode id!")
    }

    internal suspend fun getEpisodeIdAndServers(
        watchId: String,
        episode: Int?,
        season: Int?,
    ): List<Pair<String, String>> {
        val isTvShow = season != null && episode != null

        val episodeId = if (isTvShow) {
            getEpisodeId(
                watchId = watchId,
                episode = episode!!,
                season = season!!
            )
        } else watchId.split("-").last()

        val fetchServerUrl =
            if (watchId.contains("movie")) {
                "$baseUrl/ajax/episode/list/$episodeId"
            } else {
                "$baseUrl/ajax/episode/servers/$episodeId"
            }

        val response = client.request(url = fetchServerUrl).execute()
        val data = response.body?.string()
            ?: throw IllegalStateException("No available servers for this film")

        val doc = Jsoup.parse(data)
        return doc.select(".nav > li")
            .mapAsync { element ->
                val anchorElement = element.select("a")

                anchorElement.getServerName(watchId) to anchorElement.getServerUrl(baseUrl, watchId)
            }
    }
}