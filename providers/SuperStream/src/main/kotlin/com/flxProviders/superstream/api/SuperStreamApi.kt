package com.flxProviders.superstream.api

import android.content.Context
import com.flixclusive.core.network.okhttp.CloudfareWebViewInterceptor.Companion.addCloudfareVerificationInterceptor
import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.film.filter.FilterList
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.core.network.okhttp.CloudfareWebViewManager
import com.flixclusive.model.provider.MediaLink
import com.flixclusive.model.provider.Stream
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.provider.SubtitleSource
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.Movie
import com.flixclusive.model.tmdb.SearchResponseData
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.settings.ProviderSettings
import com.flxProviders.superstream.BuildConfig
import com.flxProviders.superstream.api.dto.BoxType
import com.flxProviders.superstream.api.dto.BoxType.Companion.fromFilmType
import com.flxProviders.superstream.api.dto.ExternalResponse
import com.flxProviders.superstream.api.dto.ExternalSources
import com.flxProviders.superstream.api.dto.ITEMS_PER_PAGE
import com.flxProviders.superstream.api.dto.SearchData
import com.flxProviders.superstream.api.dto.SearchData.Companion.toSearchResponseData
import com.flxProviders.superstream.api.settings.TOKEN_KEY
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
    client: OkHttpClient,
    provider: Provider,
    private val context: Context,
    private val settings: ProviderSettings
) : ProviderApi(client, provider) {
    private val name = provider.name

    private val token: String?
        get() = settings.getString(TOKEN_KEY, null)

    private val tokenHeaders: Map<String, String>
        get() = mapOf("Cookie" to "ui=$token")

    override val testFilm: FilmDetails
        get() = Movie(
            id = "47739",
            imdbId = "tt15398776",
            title = "Oppenheimer",
            homePage = null,
            posterImage = BuildConfig.SUPERSTREAM_FOURTH_API + "/uploadimg/movie/2023/07/20/2023072004020316316.jpg",
            providerName = name
        )

    override suspend fun getLinks(
        watchId: String,
        film: FilmDetails,
        episode: Episode?
    ): List<MediaLink> {
        if (token == null) {
            throw Exception("No token found! Go to $name's settings and configure it.")
        }

        return getSourceLinksFromFourthApi(
            watchId = watchId,
            filmType = fromFilmType(filmType = film.filmType),
            season = episode?.season,
            episode = episode?.number
        )
    }

    override suspend fun search(
        title: String,
        page: Int,
        id: String?,
        imdbId: String?,
        tmdbId: Int?,
        filters: FilterList,
    ): SearchResponseData<FilmSearchItem> {
        val query = imdbId ?: title
        val apiQuery = String.format(Locale.ROOT, BuildConfig.SUPERSTREAM_THIRD_API, query, page, ITEMS_PER_PAGE, Random.nextInt(0, Int.MAX_VALUE))


        val response = client.request(apiQuery).execute()
            .fromJson<SearchData>("[$name]> Couldn't search for $query")

        return response.toSearchResponseData(provider = name)
    }

    override suspend fun getFilmDetails(film: Film): FilmDetails {
        throw NotImplementedError("Not yet implemented. Please come back soon for future updates.")
    }

    private suspend fun getSourceLinksFromFourthApi(
        watchId: String,
        filmType: BoxType,
        episode: Int?,
        season: Int?
    ): List<MediaLink> {
        val firstAPI = BuildConfig.SUPERSTREAM_FIRST_API
        val secondAPI = BuildConfig.SUPERSTREAM_SECOND_API

        val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
        val shareKey = client
            .addCloudfareVerificationInterceptor(context = context)
            .request(
                url = "$firstAPI/index/share_link?id=${watchId}&type=${filmType.value}"
            ).execute().use {
                val string = it.body?.string()

                if (!it.isSuccessful || string == null) {
                    throw Exception("[$name]> Failed to fetch share key.")
                }

                fromJson<ExternalResponse>(string)
                    .data?.link?.substringAfterLast("/")
                    ?: throw Exception("[$name]> No share key found.")
            }

        val humanizedHeaders = mapOf("Accept-Language" to "en")
        val shareRes = client.request(
            url = "$secondAPI/file/file_share_list?share_key=$shareKey",
            headers = humanizedHeaders.toHeaders(),
        ).execute()
            .fromJson<ExternalResponse>("[$name]> Failed to fetch share key.").data
            ?: throw Exception("[$name]> No shared resources found (Stage 1).")

        val fids = if (season == null) {
            shareRes.fileList
        } else {
            val parentId = shareRes.fileList?.find { it.fileName.equals("season $season", true) }?.fid

            val episodesShareRes = client.request(
                url = "$secondAPI/file/file_share_list?share_key=$shareKey&parent_id=$parentId&page=1"
            ).execute()
                .fromJson<ExternalResponse>("[$name]> Failed to fetch share key.").data
                ?: throw Exception("[$name]> No shared resources found (Stage 2).")

            episodesShareRes.fileList?.filter {
                it.fileName?.contains("s${seasonSlug}e${episodeSlug}", true) == true
            }
        } ?: throw Exception("[$name]> No FIDs found.")

        val links = mutableListOf<MediaLink>()
        fids.mapAsync { fileList ->
            val player = client.request(
                url = "$secondAPI/file/player?fid=${fileList.fid}&share_key=$shareKey",
                headers = (humanizedHeaders + tokenHeaders).toHeaders()
            ).execute()
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

                            links.add(
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

                                links.add(
                                    Stream(
                                        name = "[$name]> ${source.label}",
                                        url = url
                                    )
                                )
                            }
                    }
                },
            )
        }

        return links
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