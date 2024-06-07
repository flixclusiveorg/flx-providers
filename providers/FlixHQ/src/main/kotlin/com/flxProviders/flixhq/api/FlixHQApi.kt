package com.flxProviders.flixhq.api

import android.content.Context
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.TMDBEpisode
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.dto.FilmInfo
import com.flixclusive.provider.dto.SearchResultItem
import com.flixclusive.provider.dto.SearchResults
import com.flixclusive.provider.extractor.Extractor
import com.flixclusive.provider.util.FlixclusiveWebView
import com.flixclusive.provider.util.TvShowCacheData
import com.flixclusive.provider.util.WebViewCallback
import com.flxProviders.flixhq.api.util.getEpisodeId
import com.flxProviders.flixhq.api.util.getSeasonId
import com.flxProviders.flixhq.api.util.getServerName
import com.flxProviders.flixhq.api.util.getServerUrl
import com.flxProviders.flixhq.api.util.replaceWhitespaces
import com.flxProviders.flixhq.api.util.toSearchResultItem
import com.flxProviders.flixhq.extractors.vidcloud.VidCloud
import com.flxProviders.flixhq.webview.FlixHQWebView
import com.flxProviders.flixhq.webview.util.removeAccents
import okhttp3.OkHttpClient
import org.jsoup.Jsoup

@Suppress("SpellCheckingInspection")
class FlixHQApi(
    client: OkHttpClient
) : ProviderApi(client) {
    override val baseUrl: String = "https://flixhq.to"
    override val name: String = "FlixHQ"
    override val useWebView = true

    private var tvCacheData: TvShowCacheData = TvShowCacheData()

    override suspend fun search(
        film: Film,
        page: Int
    ): SearchResults {
        val query = film.title.removeAccents()

        var searchResult = SearchResults(page, false, listOf())

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

            val results = mutableListOf<SearchResultItem>()

            doc.select(".film_list-wrap > div.flw-item").forEach { element ->
                results.add(element.toSearchResultItem(baseUrl))
            }

            return searchResult.copy(results = results)
        }

        return SearchResults()
    }

    override suspend fun getFilmInfo(
        filmId: String,
        filmType: FilmType
    ): FilmInfo {
        var filmIdToUse = filmId
        if (!filmId.startsWith(baseUrl)) {
            filmIdToUse = "$baseUrl/$filmId"
        }

        if (tvCacheData.filmInfo?.id == filmIdToUse)
            return tvCacheData.filmInfo!!

        var filmInfo = FilmInfo(
            id = filmIdToUse.split("to/").last(),
            title = ""
        )

        val response = client.request(url = filmIdToUse).execute()
        val data = response.body?.string()

        if (data != null) {
            val doc = Jsoup.parse(data)
            filmInfo = filmInfo.copy(
                title = doc.select(".heading-name > a:nth-child(1)").text()
            )

            val uid = doc.select(".watch_block").attr("data-id")
            val releaseDate = Jsoup.parse(data).select("div.row-line:nth-child(3)").text()
                .replace("Released: ", "").trim()
            filmInfo = filmInfo.copy(yearReleased = releaseDate.split("-").first())

            if (filmType == FilmType.MOVIE) {
                filmInfo = filmInfo.copy(
                    id = uid,
                    title = "${filmInfo.title} Movie",
                )
            }

            tvCacheData = TvShowCacheData(id = uid, filmInfo)
            return filmInfo
        }

        throw NullPointerException("FilmInfo is null!")
    }

    override suspend fun getSourceLinks(
        filmId: String,
        film: Film,
        season: Int?,
        episode: Int?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ) = throw IllegalAccessException("$name uses a WebView!")

    override fun getWebView(
        context: Context,
        callback: WebViewCallback,
        film: Film,
        episode: TMDBEpisode?,
    ): FlixclusiveWebView {
        return FlixHQWebView(
            mClient = client,
            api = this,
            context = context,
            filmToScrape = film,
            episodeData = episode,
            callback = callback
        )
    }

    internal fun getEpisodeId(
        filmId: String,
        episode: Int,
        season: Int,
    ): String {
        val filmIdToUse = filmId.split("-").last()
        val isSameId = tvCacheData.id == filmIdToUse

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
                client.request(url = ajaxReqUrl(filmIdToUse, true)).execute()
            val dataSeasons = responseSeasons.body?.string()
                ?: throw Exception("Failed to fetch season data from provider")

            val seasonsDoc = Jsoup.parse(dataSeasons)
                .select(".dropdown-menu > a")

            tvCacheData = if (isSameId) {
                tvCacheData.copy(seasons = seasonsDoc)
            } else {
                TvShowCacheData(id = filmIdToUse, seasons = seasonsDoc)
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
                id = filmIdToUse,
                seasons = seasons,
                episodes = episodes
            )
        }

        return episodes.getEpisodeId(episode) ?: throw Exception("Cannot find episode id!")
    }

    internal suspend fun getEpisodeIdAndServers(
        filmId: String,
        episode: Int?,
        season: Int?,
    ): Pair<String, List<Pair<String, String>>> {
        val isTvShow = season != null && episode != null

        val episodeId = if (isTvShow) {
            getEpisodeId(
                filmId = filmId,
                episode = episode!!,
                season = season!!
            )
        } else filmId.split("-").last()

        val fetchServerUrl =
            if (filmId.contains("movie")) {
                "$baseUrl/ajax/episode/list/$episodeId"
            } else {
                "$baseUrl/ajax/episode/servers/$episodeId"
            }

        val response = client.request(url = fetchServerUrl).execute()
        val data = response.body?.string()
            ?: throw IllegalStateException("No available servers for this film")

        val doc = Jsoup.parse(data)
        return episodeId to doc.select(".nav > li")
            .mapAsync { element ->
                val anchorElement = element.select("a")

                anchorElement.getServerName(filmId) to anchorElement.getServerUrl(baseUrl, filmId)
            }
    }
}