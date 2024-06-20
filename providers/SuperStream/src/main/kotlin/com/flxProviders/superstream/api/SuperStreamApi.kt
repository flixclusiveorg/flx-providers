package com.flxProviders.superstream.api

import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.provider.SubtitleSource
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.SearchResponseData
import com.flixclusive.provider.ProviderApi
import com.flxProviders.superstream.BuildConfig
import com.flxProviders.superstream.api.dto.ExternalResponse
import com.flxProviders.superstream.api.dto.ExternalSources
import com.flxProviders.superstream.api.dto.ITEMS_PER_PAGE
import com.flxProviders.superstream.api.dto.SearchData
import com.flxProviders.superstream.api.dto.SearchData.Companion.toSearchResponseData
import com.flxProviders.superstream.api.util.SuperStreamUtil
import com.flxProviders.superstream.api.util.SuperStreamUtil.BoxType.Companion.fromFilmType
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
    companion object {
        internal const val DEFAULT_PROVIDER_NAME = "SuperStream"
    }

    override val name: String
        get() = DEFAULT_PROVIDER_NAME

    override suspend fun getSourceLinks(
        watchId: String,
        film: FilmDetails,
        season: Int?,
        episode: Int?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
        client.getSourceLinksFromFourthApi(
            watchId = watchId,
            filmType = fromFilmType(filmType = film.filmType),
            season = season,
            episode = episode,
            onSubtitleLoaded = onSubtitleLoaded,
            onLinkLoaded = onLinkLoaded
        )
    }

    override suspend fun search(
        title: String,
        page: Int,
        id: String?,
        imdbId: String?,
        tmdbId: Int?
    ): SearchResponseData<FilmSearchItem> {
        val query = imdbId ?: title
        val apiQuery = String.format(Locale.ROOT, BuildConfig.SUPERSTREAM_THIRD_API, query, ITEMS_PER_PAGE, Random.nextInt(0, Int.MAX_VALUE))


        val response = client.request(apiQuery).execute()
            .fromJson<SearchData>("[$name]> Couldn't search for $tmdbId")

        return response.toSearchResponseData()
    }

    private suspend fun OkHttpClient.getSourceLinksFromFourthApi(
        watchId: String,
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