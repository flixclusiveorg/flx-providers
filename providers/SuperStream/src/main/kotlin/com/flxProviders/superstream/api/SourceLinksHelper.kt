package com.flxProviders.superstream.api

import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.provider.SubtitleSource
import com.flixclusive.provider.dto.FilmInfo
import com.flxProviders.superstream.BuildConfig
import com.flxProviders.superstream.api.dto.ExternalResponse
import com.flxProviders.superstream.api.dto.ExternalSources
import com.flxProviders.superstream.api.dto.SuperStreamDownloadResponse
import com.flxProviders.superstream.api.dto.SuperStreamSubtitleResponse
import com.flxProviders.superstream.api.dto.SuperStreamSubtitleResponse.SuperStreamSubtitleItem.Companion.toValidSubtitleFilePath
import com.flxProviders.superstream.api.util.Constants
import com.flxProviders.superstream.api.util.SuperStreamUtil
import com.flxProviders.superstream.api.util.SuperStreamUtil.raiseOnError
import com.flxProviders.superstream.api.util.superStreamCall
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import org.jsoup.Jsoup

internal suspend fun OkHttpClient.getSourceLinksFromSecondApi(
    filmId: String,
    season: Int?,
    episode: Int?,
    tvShowInfo: FilmInfo? = null,
    onSubtitleLoaded: (Subtitle) -> Unit,
    onLinkLoaded: (SourceLink) -> Unit
) {
    val isMovie = season == null && episode == null

    val seasonToUse = if(
        tvShowInfo?.seasons != null
        && tvShowInfo.episodes != null
        && tvShowInfo.seasons!! <= season!!
        && tvShowInfo.episodes!! >= episode!!
    ) {
        tvShowInfo.seasons
    } else season

    val query = if (isMovie) {
        """{"childmode":"0","uid":"","app_version":"${Constants.APP_VERSION}","appid":"${Constants.appIdSecond}","module":"Movie_downloadurl_v3","channel":"Website","mid":"$filmId","lang":"en","expired_date":"${SuperStreamUtil.getExpiryDate()}","platform":"android","oss":"1","group":""}"""
    } else {
        """{"childmode":"0","app_version":"${Constants.APP_VERSION}","module":"TV_downloadurl_v3","channel":"Website","episode":"$episode","expired_date":"${SuperStreamUtil.getExpiryDate()}","platform":"android","tid":"$filmId","oss":"1","uid":"","appid":"${Constants.appIdSecond}","season":"$seasonToUse","lang":"en","group":""}"""
    }

    val downloadResponse = superStreamCall<SuperStreamDownloadResponse>(query, true)

    downloadResponse?.msg?.raiseOnError("[SuperStream 1]> Failed to fetch source.")

    val data = downloadResponse?.data?.list?.randomOrNull()
        ?: throw Exception("[SuperStream 1]> Cannot find source")

    // Should really run this query for every link :(
    val subtitleQuery = if (isMovie) {
        """{"childmode":"0","fid":"${data.fid}","uid":"","app_version":"${Constants.APP_VERSION}","appid":"${Constants.appIdSecond}","module":"Movie_srt_list_v2","channel":"Website","mid":"$filmId","lang":"en","expired_date":"${SuperStreamUtil.getExpiryDate()}","platform":"android"}"""
    } else {
        """{"childmode":"0","fid":"${data.fid}","app_version":"${Constants.APP_VERSION}","module":"TV_srt_list_v2","channel":"Website","episode":"$episode","expired_date":"${SuperStreamUtil.getExpiryDate()}","platform":"android","tid":"$filmId","uid":"","appid":"${Constants.appIdSecond}","season":"$seasonToUse","lang":"en"}"""
    }

    val subtitlesResponse = superStreamCall<SuperStreamSubtitleResponse>(subtitleQuery)
    subtitlesResponse?.msg?.raiseOnError("[SuperStream 1]> Failed to fetch subtitles.")

    asyncCalls(
        {
            subtitlesResponse?.data?.list?.mapAsync { subtitle ->
                subtitle.subtitles
                    .sortedWith(compareByDescending { it.order })
                    .mapAsync {
                        if(
                            it.filePath != null
                            && it.lang != null
                        ) {
                            onSubtitleLoaded(
                                Subtitle(
                                    language = "${it.language ?: "UNKNOWN"} [${it.lang}] - Votes: ${it.order}",
                                    url = it.filePath.toValidSubtitleFilePath(),
                                    type = SubtitleSource.ONLINE
                                )
                            )
                        }
                    }
            }
        },
        {
            downloadResponse.data.list.mapAsync {
                if(
                    !it.path.isNullOrBlank()
                    && !it.realQuality.isNullOrBlank()
                ) {
                    onLinkLoaded(
                        SourceLink(
                            name = "[SuperStream 1]> ${it.realQuality}",
                            url = it.path
                        )
                    )
                }
            }
        }
    )
}

internal suspend fun OkHttpClient.getSourceLinksFromFourthApi(
    filmId: String,
    filmType: SuperStreamUtil.SSMediaType,
    season: Int?,
    episode: Int?,
    onSubtitleLoaded: (Subtitle) -> Unit,
    onLinkLoaded: (SourceLink) -> Unit,
) {
    val fourthAPI = BuildConfig.SUPERSTREAM_FOURTH_API
    val thirdAPI = BuildConfig.SUPERSTREAM_THIRD_API

    val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
    val shareKey = request(
        url = "$fourthAPI/index/share_link?id=${filmId}&type=${filmType.value}"
    ).execute().use {
        val string = it.body?.string()

        if (!it.isSuccessful || string == null) {
            throw Exception("[SuperStream 2]> Failed to fetch share key.")
        }

        fromJson<ExternalResponse>(string)
            .data?.link?.substringAfterLast("/")
            ?: throw Exception("[SuperStream 2]> No share key found.")
    }

    val headers = mapOf("Accept-Language" to "en")
    val shareRes = request(
        url = "$thirdAPI/file/file_share_list?share_key=$shareKey",
        headers = headers.toHeaders(),
    ).execute().use {
        val string = it.body?.string()

        if (!it.isSuccessful || string == null) {
            throw Exception("[SuperStream 2]> Failed to fetch share key.")
        }

        fromJson<ExternalResponse>(string).data
            ?: throw Exception("[SuperStream 2]> No shared resources found.")
    }

    val fids = if (season == null) {
        shareRes.fileList
    } else {
        val parentId = shareRes.fileList?.find { it.fileName.equals("season $season", true) }?.fid

        val episodesShareRes = request(
            url = "$thirdAPI/file/file_share_list?share_key=$shareKey&parent_id=$parentId&page=1"
        ).execute().use {
            val string = it.body?.string()

            if (!it.isSuccessful || string == null) {
                throw Exception("[SuperStream 2]> Failed to fetch share key.")
            }

            fromJson<ExternalResponse>(string).data
                ?: throw Exception("[SuperStream 2]> No shared resources found.")
        }

        episodesShareRes.fileList?.filter {
            it.fileName?.contains("s${seasonSlug}e${episodeSlug}", true) == true
        }
    } ?: throw Exception("[SuperStream 2]> No FIDs found.")

    fids.mapAsync { fileList ->
        val player = request("$thirdAPI/file/player?fid=${fileList.fid}&share_key=$shareKey").execute()
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
                                    name = "[SuperStream 2]> ${source.label}",
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