package com.flxProviders.superstream

import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmDetails
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.filter.FilterList
import com.flxProviders.superstream.dto.CommonResponse
import com.flxProviders.superstream.dto.MediaMetadata
import com.flxProviders.superstream.dto.MediaMetadata.Companion.toFilmDetails
import com.flxProviders.superstream.dto.SearchData
import com.flxProviders.superstream.dto.SearchItem.Companion.toFilmSearchItem
import com.flxProviders.superstream.dto.StreamData
import com.flxProviders.superstream.dto.SubtitleData
import com.flxProviders.superstream.dto.SubtitleItem.Companion.toValidSubtitleFilePath
import com.flxProviders.superstream.util.Constants.APP_ID
import com.flxProviders.superstream.util.Constants.APP_VERSION
import com.flxProviders.superstream.util.SuperStreamUtil.getExpiryDate
import com.flxProviders.superstream.util.SuperStreamUtil.raiseOnError
import com.flxProviders.superstream.util.SuperStreamUtil.superStreamCall
import okhttp3.OkHttpClient

class SuperStreamTestApi(
    client: OkHttpClient,
    provider: Provider
) : ProviderApi(client, provider) {

    override suspend fun getFilmDetails(film: Film): FilmDetails {
        val apiQuery = if (film.filmType == FilmType.MOVIE) {
            """{"childmode":"0","uid":"","app_version":"$APP_VERSION","appid":"$APP_ID","module":"Movie_detail","channel":"Website","mid":"${film.id}","lang":"en","expired_date":"${getExpiryDate()}","platform":"android","oss":"","group":""}"""
        } else {
            """{"childmode":"0","uid":"","app_version":"$APP_VERSION","appid":"$APP_ID","module":"TV_detail_1","display_all":"1","channel":"Website","lang":"en","expired_date":"${getExpiryDate()}","platform":"android","tid":"${film.id}"}"""
        }

        val response = client.superStreamCall<CommonResponse<MediaMetadata>>(apiQuery)
        response?.msg?.raiseOnError("Failed to fetch movie info.")

        return response?.data!!.toFilmDetails()
    }

    override suspend fun search(
        title: String,
        page: Int,
        id: String?,
        imdbId: String?,
        tmdbId: Int?,
        filters: FilterList
    ): SearchResponseData<FilmSearchItem> {
        val itemsPerPage = 20
        val apiQuery =
            // Originally 8 pagelimit
            """{"childmode":"0","app_version":"$APP_VERSION","appid":"$APP_ID","module":"Search5","channel":"Website","page":"$page","lang":"en","type":"all","keyword":"${imdbId ?: title}","pagelimit":"$itemsPerPage","expired_date":"${getExpiryDate()}","platform":"android"}"""

        val response = client.superStreamCall<SearchData>(query = apiQuery)

        val mappedItems = response?.results?.map {
            it.toFilmSearchItem()
        } ?: throw NullPointerException("Cannot search on SuperStream")

        return SearchResponseData(
            page = page,
            results = mappedItems,
            hasNextPage = (page * itemsPerPage) < response.total
        )
    }

    override suspend fun getLinks(
        watchId: String,
        film: FilmDetails,
        episode: Episode?,
        onLinkFound: (MediaLink) -> Unit
    ) {
        val season = episode?.season
        val episodeNumber = episode?.number

        val isMovie = episode == null

        val tvShowInfo = if(season != null) {
            getFilmDetails(film) as TvShow
        } else null

        val seasonToUse = if(
            tvShowInfo?.totalSeasons != null
            && tvShowInfo.totalSeasons <= season!!
            && tvShowInfo.totalEpisodes >= episodeNumber!!
        ) {
            tvShowInfo.totalSeasons
        } else season

        val query = if (isMovie) {
            """{"childmode":"0","uid":"14710508","app_version":"$APP_VERSION","appid":"$APP_ID","module":"Movie_downloadurl","channel":"Website","mid":"$watchId","lang":"en","expired_date":"${getExpiryDate()}","platform":"android","oss":"1","group":""}"""
        } else {
            """{"childmode":"0","app_version":"$APP_VERSION","module":"TV_downloadurl","channel":"Website","episode":"$episodeNumber","expired_date":"${getExpiryDate()}","platform":"android","tid":"$watchId","oss":"1","uid":"14710508","appid":"$APP_ID","season":"$seasonToUse","lang":"en","group":""}"""
        }

        val downloadResponse = client.superStreamCall<CommonResponse<StreamData>>(query)
        downloadResponse?.msg?.raiseOnError("Failed to fetch source.")

        val validItem = downloadResponse?.data?.list
            ?.firstOrNull { it.fid != null }
            ?: throw Exception("Cannot find source")

        // Should really run this query for every link :(
        val subtitleQuery = if (isMovie) {
            """{"childmode":"0","fid":"${validItem.fid}","uid":"14710508","app_version":"$APP_VERSION","appid":"$APP_ID","module":"Movie_srt_list_v2","channel":"Website","mid":"$watchId","lang":"en","expired_date":"${getExpiryDate()}","platform":"android"}"""
        } else {
            """{"childmode":"0","fid":"${validItem.fid}","app_version":"$APP_VERSION","module":"TV_srt_list_v2","channel":"Website","episode":"$episodeNumber","expired_date":"${getExpiryDate()}","platform":"android","tid":"$watchId","uid":"14710508","appid":"$APP_ID","season":"$seasonToUse","lang":"en"}"""
        }

        val subtitlesResponse = client.superStreamCall<CommonResponse<SubtitleData>>(subtitleQuery)
        subtitlesResponse?.msg?.raiseOnError("Failed to fetch subtitles.")

        asyncCalls(
            {
                subtitlesResponse?.data?.list?.forEach topForEach@ { item ->
                    item.subtitles
                        .sortedWith(compareByDescending { it.order })
                        .forEach {
                            val validFile = it.filePath != null && it.lang != null
                            if (!validFile)
                                return@topForEach

                            val subtitle = Subtitle(
                                language = """
                                    [SS] ${it.language ?: "UNKNOWN"} (${it.lang}) 
                                    Votes 👍: ${it.order}
                                """.trimIndent(),
                                url = it.filePath!!.toValidSubtitleFilePath()
                            )
                            onLinkFound(subtitle)
                        }
                }
            },
            {
                downloadResponse.data.list.forEach { item ->
                    val stream = if (!item.path.isNullOrBlank()) {
                        Stream(
                            name = """
                                [SS] ${item.quality}
                                Views 👁️: ${item.count}
                                File 📹: ${item.filename}
                                Size 💾: ${item.size}
                            """.trimIndent(),
                            url = item.path!!
                        )
                    } else {
                        Stream(
                            name = """
                                [SS] ${item.quality}
                                Views 👁️: ${item.count}
                                File 📹: ${item.filename}
                                Size 💾: ${item.size}
                            """.trimIndent(),
                            url = item.path!!
                        )
                    }

                    onLinkFound(stream)
                }
            }
        )
    }

    private fun getExternalStream(fid: String) {

    }
}