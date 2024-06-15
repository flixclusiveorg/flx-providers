package com.flxProviders.superstream.api

import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.provider.SubtitleSource
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.Movie
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.dto.SearchResultItem
import com.flixclusive.provider.dto.SearchResults
import com.flxProviders.superstream.BuildConfig
import com.flxProviders.superstream.api.dto.ExternalResponse
import com.flxProviders.superstream.api.dto.ExternalSources
import com.flxProviders.superstream.api.dto.ITEMS_PER_PAGE
import com.flxProviders.superstream.api.dto.SearchData
import com.flxProviders.superstream.api.dto.TmdbQueryDto
import com.flxProviders.superstream.api.util.SuperStreamUtil
import com.flxProviders.superstream.api.util.SuperStreamUtil.BoxType.Companion.fromFilmType
import com.flxProviders.superstream.api.util.getTmdbQuery
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import java.util.Locale
import kotlin.random.Random

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
        val mediaId = getMediaId(
            tmdbId = filmId,
            filmType = film.filmType
        )

        client.getSourceLinksFromFourthApi(
            filmId = mediaId,
            filmType = fromFilmType(filmType = film.filmType),
            season = season,
            episode = episode,
            onSubtitleLoaded = onSubtitleLoaded,
            onLinkLoaded = onLinkLoaded
        )
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
            currentPage = page,
            results = listOf(
                SearchResultItem(
                    id = film.id.toString(),
                    tmdbId = film.id,
                )
            ),
            hasNextPage = false
        )
    }

    private fun getMediaId(
        tmdbId: String,
        filmType: FilmType
    ): String {
        val imdbId = getImdbId(tmdbId, filmType)
        val apiQuery = String.format(Locale.ROOT, BuildConfig.SUPERSTREAM_THIRD_API, imdbId, ITEMS_PER_PAGE, Random.nextInt(0, Int.MAX_VALUE))


        val response = client.request(apiQuery).execute()
            .fromJson<SearchData>("[$name]> Couldn't search for $tmdbId")

        val id = response.data.results.find {
            it.imdbId.equals(imdbId, true)
        }?.id ?: throw Exception("[$name]> Film with ID $imdbId was not found.")

        return id
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

    private suspend fun OkHttpClient.getSourceLinksFromFourthApi(
        filmId: String,
        filmType: SuperStreamUtil.BoxType,
        season: Int?,
        episode: Int?,
        onSubtitleLoaded: (Subtitle) -> Unit,
        onLinkLoaded: (SourceLink) -> Unit,
    ) {
        val firstAPI = BuildConfig.SUPERSTREAM_FIRST_API
        val secondAPI = BuildConfig.SUPERSTREAM_SECOND_API

        val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
        val shareKey = request(
            url = "$firstAPI/index/share_link?id=${filmId}&type=${filmType.value}"
        ).execute().use {
            val string = it.body?.string()

            if (!it.isSuccessful || string == null) {
                throw Exception("[$name]> Failed to fetch share key.")
            }

            fromJson<ExternalResponse>(string)
                .data?.link?.substringAfterLast("/")
                ?: throw Exception("[$name]> No share key found.")
        }

        val headers = mapOf("Accept-Language" to "en")
        val shareRes = request(
            url = "$secondAPI/file/file_share_list?share_key=$shareKey",
            headers = headers.toHeaders(),
        ).execute().use {
            val string = it.body?.string()

            if (!it.isSuccessful || string == null) {
                throw Exception("[$name]> Failed to fetch share key.")
            }

            fromJson<ExternalResponse>(string).data
                ?: throw Exception("[$name]> No shared resources found.")
        }

        val fids = if (season == null) {
            shareRes.fileList
        } else {
            val parentId = shareRes.fileList?.find { it.fileName.equals("season $season", true) }?.fid

            val episodesShareRes = request(
                url = "$secondAPI/file/file_share_list?share_key=$shareKey&parent_id=$parentId&page=1"
            ).execute().use {
                val string = it.body?.string()

                if (!it.isSuccessful || string == null) {
                    throw Exception("[$name]> Failed to fetch share key.")
                }

                fromJson<ExternalResponse>(string).data
                    ?: throw Exception("[$name]> No shared resources found.")
            }

            episodesShareRes.fileList?.filter {
                it.fileName?.contains("s${seasonSlug}e${episodeSlug}", true) == true
            }
        } ?: throw Exception("[$name]> No FIDs found.")

        fids.mapAsync { fileList ->
            val player = request("$secondAPI/file/player?fid=${fileList.fid}&share_key=$shareKey").execute()
                .body?.string()
                ?: return@mapAsync

            val subtitles = Regex("""\$\(".jw-wrapper"\).prepend\('(.*)'\)""")
                .find(player)
                ?.groupValues
                ?.get(1)
            val sources = Regex("sources\\s*=\\s*(.*);")
                .find(player)
                ?.groupValues
                ?.get(1)
            val qualities = Regex("quality_list\\s*=\\s*(.*);")
                .find(player)
                ?.groupValues
                ?.get(1)

            asyncCalls(
                {
                    if (subtitles == null)
                        return@asyncCalls

                    val jsoupObject = Jsoup.parse(subtitles)

                    val div = jsoupObject.selectFirst("div.right2") ?: return@asyncCalls
                    val languages = div.select("ul")

                    languages.forEach { languageElement ->
                        val language = languageElement.id()
                        val subtitleLinks = languageElement.select("li")

                        subtitleLinks.forEachIndexed { i, subtitleLink ->
                            val link = subtitleLink.attr("data-url")

                            val identifier = if (i == 0) "" else "$i"

                            onSubtitleLoaded(
                                Subtitle(
                                    language = "$language [$identifier]",
                                    url = link,
                                    type = SubtitleSource.ONLINE
                                )
                            )
                        }
                    }

                },
                {
                    listOf(sources, qualities).forEach { item ->
                        if (item == null)
                            return@forEach

                        fromJson<List<ExternalSources>>(item)
                            .forEach org@ { source ->

                                val isNotAutoAndVideo = !source.label.equals("AUTO", true) && !source.type.equals("video/mp4", true)

                                if(isNotAutoAndVideo)
                                    return@org

                                val url = (source.hlsUrl ?: source.file)
                                    ?.replace("\\/", "/")
                                    ?: return@org

                                onLinkLoaded(
                                    SourceLink(
                                        name = "[$name]> ${source.label}",
                                        url = url
                                    )
                                )
                            }
                    }
                },
            )
        }
    }

    private fun getEpisodeSlug(
        season: Int? = null,
        episode: Int? = null,
    ): Pair<String, String> {
        if (season == null && episode == null) {
            return "" to ""
        }

        val seasonSlug = when {
            season!! < 10 -> "0$season"
            else -> "$season"
        }
        val episodeSlug = when {
            episode!! < 10 -> "0$episode"
            else -> "$episode"
        }

        return seasonSlug to episodeSlug
    }
}