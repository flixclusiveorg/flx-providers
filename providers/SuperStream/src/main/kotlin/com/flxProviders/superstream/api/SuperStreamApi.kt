package com.flxProviders.superstream.api

import android.content.Context
import android.content.res.Resources
import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmDetails
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.model.provider.link.SubtitleSource
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.filter.FilterList
import com.flixclusive.provider.settings.ProviderSettings
import com.flxProviders.superstream.BuildConfig
import com.flxProviders.superstream.api.dto.BoxType
import com.flxProviders.superstream.api.dto.ExternalResponse
import com.flxProviders.superstream.api.dto.ExternalSources
import com.flxProviders.superstream.api.dto.SearchData
import com.flxProviders.superstream.api.dto.old.CommonResponse
import com.flxProviders.superstream.api.dto.old.MediaMetadata
import com.flxProviders.superstream.api.dto.old.MediaMetadata.Companion.toFilmDetails
import com.flxProviders.superstream.api.util.ResourceUtil.getRawFileInputStream
import com.flxProviders.superstream.api.util.old.CLIENT_CA4_NAME
import com.flxProviders.superstream.api.util.old.CLIENT_CA_NAME
import com.flxProviders.superstream.api.util.old.CLIENT_CERT_NAME
import com.flxProviders.superstream.api.util.old.CLIENT_KEY_NAME
import com.flxProviders.superstream.api.util.old.Constants.APP_ID
import com.flxProviders.superstream.api.util.old.Constants.APP_VERSION
import com.flxProviders.superstream.api.util.old.CustomCertificateClient
import com.flxProviders.superstream.api.util.old.SuperStreamUtil.getExpiryDate
import com.flxProviders.superstream.api.util.old.SuperStreamUtil.raiseOnError
import com.flxProviders.superstream.api.util.old.SuperStreamUtil.superStreamCall
import com.flxProviders.superstream.settings.TOKEN_KEY
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import org.jsoup.Jsoup

class SuperStreamApi(
    client: OkHttpClient,
    provider: Provider,
    resources: Resources,
    val context: Context,
    private val settings: ProviderSettings
) : ProviderApi(client, provider) {
    private val token: String?
        get() = settings.getString(TOKEN_KEY, null)

    private val tokenHeaders: Map<String, String>
        get() = mapOf("Cookie" to "ui=$token")

    private val specialClient: OkHttpClient

    init {
        val certStream = getRawFileInputStream(resources, CLIENT_CERT_NAME)
        val keyStream = getRawFileInputStream(resources, CLIENT_KEY_NAME)
        val caStream = getRawFileInputStream(resources, CLIENT_CA_NAME)
        val ca4Stream = getRawFileInputStream(resources, CLIENT_CA4_NAME)

        val certificateClient = CustomCertificateClient()

        specialClient = certificateClient.createOkHttpClient(
            clientCertStream = certStream,
            caCertStreams = listOf(caStream, ca4Stream),
            privateKeyInputStream = keyStream,
            privateKeyPassword = null
        )
    }

    override val testFilm: FilmDetails
        get() = Movie(
            id = "55137",
            imdbId = "tt22022452",
            title = "Inside Out 2",
            homePage = null,
            posterImage = "${BuildConfig.SUPERSTREAM_FOURTH_API}/uploadimg/movie/2024/06/11/2024061123000673838.jpg",
            providerName = provider.name
        )

    override suspend fun getLinks(
        watchId: String,
        film: FilmDetails,
        episode: Episode?,
        onLinkFound: (MediaLink) -> Unit
    ) {
        requireNotNull(token) { "No token found! Go to ${provider.name}'s settings and configure it." }

        getSourceLinksFromFourthApi(
            watchId = watchId,
            filmType = BoxType.fromFilmType(film.filmType),
            season = episode?.season,
            episode = episode?.number,
            onLinkFound = onLinkFound
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
        val itemsPerPage = 20
        val apiQuery =
            // Originally 8 pagelimit
            """{"childmode":"0","app_version":"$APP_VERSION","appid":"$APP_ID","module":"Search3","channel":"Website","page":"$page","lang":"en","type":"all","keyword":"${imdbId ?: title}","pagelimit":"$itemsPerPage","expired_date":"${getExpiryDate()}","platform":"android"}"""

        val response = specialClient.superStreamCall<SearchData>(query = apiQuery)

        val mappedItems = response?.results?.map {
            it.toFilmSearchItem(
                provider = provider.name,
                imdbId = imdbId,
                tmdbId = tmdbId
            )
        } ?: throw NullPointerException("Cannot search on SuperStream")

        return SearchResponseData(
            page = page,
            results = mappedItems,
            hasNextPage = (page * itemsPerPage) < response.total
        )
    }

    override suspend fun getFilmDetails(film: Film): FilmDetails {
        val apiQuery = if (film.filmType == FilmType.MOVIE) {
            """{"childmode":"0","uid":"","app_version":"$APP_VERSION","appid":"$APP_ID","module":"Movie_detail","channel":"Website","mid":"${film.id}","lang":"en","expired_date":"${getExpiryDate()}","platform":"android","oss":"","group":""}"""
        } else {
            """{"childmode":"0","uid":"","app_version":"$APP_VERSION","appid":"$APP_ID","module":"TV_detail_1","display_all":"1","channel":"Website","lang":"en","expired_date":"${getExpiryDate()}","platform":"android","tid":"${film.id}"}"""
        }

        val response = specialClient.superStreamCall<CommonResponse<MediaMetadata>>(apiQuery)
        response?.msg?.raiseOnError("Failed to fetch movie info.")

        return response?.data!!.toFilmDetails()
    }

    private suspend fun getSourceLinksFromFourthApi(
        watchId: String,
        filmType: BoxType,
        episode: Int?,
        season: Int?,
        onLinkFound: (MediaLink) -> Unit
    ) {
        val shareKey = fetchShareKey(
            watchId = watchId, filmType = filmType
        )
        val sharedResources = fetchSharedResources(shareKey)
        val files = fetchFiles(
            sharedResources = sharedResources,
            season = season,
            episode = episode,
            shareKey = shareKey
        )
        val cleanedFiles = cleanFiles(
            files = files, shareKey = shareKey
        )

        processFiles(
            cleanedFiles = cleanedFiles,
            shareKey = shareKey,
            onLinkFound = onLinkFound
        )
    }

    private fun fetchShareKey(watchId: String, filmType: BoxType): String {
//        val interceptor = withMainContext {
//            CloudflareWebViewInterceptor(context)
//        }

//        val url =
//            "${BuildConfig.SUPERSTREAM_FIRST_API}/index/share_link?id=$watchId&type=${filmType.value}"
        val url =
            "${BuildConfig.SUPERSTREAM_SECOND_API}/mbp/to_share_page?box_type=${filmType.value}&mid=$watchId&json=1"

        val response = client
//            .addWebViewInterceptor(interceptor)
            .request(url)
            .execute()

        val parsedResponse = response.fromJson<ExternalResponse>(
            errorMessage = "[${provider.name}] Failed to fetch share key."
        )

        return parsedResponse.data?.link?.substringAfterLast("/")
            ?: throw Exception("[${provider.name}] No share key found.")
    }

    private fun fetchSharedResources(shareKey: String): ExternalResponse.Data {
        val url = "${BuildConfig.SUPERSTREAM_SECOND_API}/file/file_share_list?share_key=$shareKey"
        val humanizedHeaders = mapOf("Accept-Language" to "en")
        return client.request(url, headers = humanizedHeaders.toHeaders())
            .execute()
            .fromJson<ExternalResponse>("[${provider.name}] Can't fetch file lists.")
            .data ?: throw Exception("[${provider.name}] No shared resources found (Stage 1).")
    }

    private fun fetchFiles(
        sharedResources: ExternalResponse.Data,
        season: Int?,
        episode: Int?,
        shareKey: String
    ): List<ExternalResponse.Data.StreamFile> {
        return if (season == null) {
            sharedResources.files
        } else {
            fetchSeasonFiles(
                sharedResources = sharedResources,
                season = season,
                episode = episode!!,
                shareKey = shareKey
            )
        } ?: throw Exception("[${provider.name}] No FIDs found.")
    }

    private fun fetchSeasonFiles(
        sharedResources: ExternalResponse.Data,
        season: Int,
        episode: Int,
        shareKey: String
    ): List<ExternalResponse.Data.StreamFile>? {
        val parentId = findSeasonParentId(sharedResources, season, shareKey)
        val url =
            "${BuildConfig.SUPERSTREAM_SECOND_API}/file/file_share_list?share_key=$shareKey&parent_id=$parentId&page=1"
        val episodesShareRes = client.request(url)
            .execute()
            .fromJson<ExternalResponse>("[${provider.name}] Failed to fetch share key.")
            .data ?: throw Exception("[${provider.name}] No shared resources found (Stage 2).")

        val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
        return episodesShareRes.files?.filter {
            it.fileName.contains("s${seasonSlug}e${episodeSlug}", true)
        }
    }

    private fun findSeasonParentId(
        sharedResources: ExternalResponse.Data,
        season: Int,
        shareKey: String
    ): Long {
        var parentId = sharedResources.files
            ?.find { it.fileName.equals("season $season", true) }?.fid

        if (parentId == null && sharedResources.files?.isNotEmpty() == true) {
            val subDirectoryId = sharedResources.files.first().fid
            parentId = fetchSubDirectorySeasonId(subDirectoryId, season, shareKey)
        }

        return requireNotNull(parentId) { "[${provider.name}] No shared resources found (Stage 2)." }
    }

    private fun fetchSubDirectorySeasonId(
        subDirectoryId: Long?,
        season: Int,
        shareKey: String
    ): Long? {
        val url =
            "${BuildConfig.SUPERSTREAM_SECOND_API}/file/file_share_list?share_key=$shareKey&parent_id=$subDirectoryId&page=1"
        return client.request(url)
            .execute()
            .fromJson<ExternalResponse>("[${provider.name}] Failed to fetch share key.")
            .data?.files
            ?.find { it.fileName.equals("season $season", true) }?.fid
    }

    private suspend fun cleanFiles(
        files: List<ExternalResponse.Data.StreamFile>,
        shareKey: String
    ): List<ExternalResponse.Data.StreamFile> {
        val cleanedFiles = mutableListOf<ExternalResponse.Data.StreamFile>()
        files.mapAsync {
            if (it.isDirectory) {
                val filesFromDirectory = fetchFilesFromDirectory(it.fid, shareKey)
                cleanedFiles.addAll(filesFromDirectory)
            } else {
                cleanedFiles.add(it)
            }
        }
        return cleanedFiles
    }

    private fun fetchFilesFromDirectory(
        fid: Long?,
        shareKey: String
    ): List<ExternalResponse.Data.StreamFile> {
        val url =
            "${BuildConfig.SUPERSTREAM_SECOND_API}/file/file_share_list?share_key=$shareKey&parent_id=$fid"
        val humanizedHeaders = mapOf("Accept-Language" to "en")
        return client.request(url, headers = humanizedHeaders.toHeaders())
            .execute()
            .fromJson<ExternalResponse>("[${provider.name}] Failed to fetch directory files.")
            .data?.files
            ?: throw Exception("[${provider.name}] No files found on this directory.")
    }

    private suspend fun processFiles(
        cleanedFiles: List<ExternalResponse.Data.StreamFile>,
        shareKey: String,
        onLinkFound: (MediaLink) -> Unit
    ) {
        cleanedFiles.forEach { fileList ->
            val playerUrl =
                "${BuildConfig.SUPERSTREAM_SECOND_API}/file/player?fid=${fileList.fid}&share_key=$shareKey"
            val player = fetchPlayerContent(playerUrl)
            val (subtitles, sources, qualities) = extractPlayerData(player)

            asyncCalls(
                {
                    processSubtitles(
                        subtitles = subtitles,
                        onLinkFound = onLinkFound
                    )
                },
                {
                    processSources(
                        sources = sources,
                        qualities = qualities,
                        file = fileList,
                        onLinkFound = onLinkFound
                    )
                }
            )
        }
    }

    private fun fetchPlayerContent(playerUrl: String): String {
        val humanizedHeaders = mapOf("Accept-Language" to "en")
        return client.request(
            url = playerUrl,
            headers = (humanizedHeaders + tokenHeaders).toHeaders()
        ).execute().body?.string()
            ?: throw Exception("[${provider.name}] Failed to fetch player content.")
    }

    private fun extractPlayerData(player: String): Triple<String?, String?, String?> {
        val subtitles =
            Regex("""\$\(".jw-wrapper"\).prepend\('(.*)'\)""").find(player)?.groupValues?.get(1)
        val sources = Regex("sources\\s*=\\s*(.*);").find(player)?.groupValues?.get(1)
        val qualities = Regex("quality_list\\s*=\\s*(.*);").find(player)?.groupValues?.get(1)
        return Triple(subtitles, sources, qualities)
    }

    private fun processSubtitles(
        subtitles: String?,
        onLinkFound: (MediaLink) -> Unit
    ) {
        if (subtitles == null) return

        val jsoupObject = Jsoup.parse(subtitles)
        val div = jsoupObject.selectFirst("div.right2") ?: return
        val languages = div.select("ul")

        languages.forEach { languageElement ->
            val languageId = languageElement.id() ?: "UNKNOWN"
            val subtitleLinks = languageElement.select("li")

            subtitleLinks.forEach { subtitleLink ->
                val link = subtitleLink.attr("data-url")
                val fileName = subtitleLink.selectFirst("p")?.text() ?: "UNKNOWN SUBTITLE"
                val subtitle = Subtitle(
                    language = """
                        Subtitle 🗯️: $fileName
                        Language 🌐: $languageId
                    """.trimIndent(),
                    url = link,
                    type = SubtitleSource.ONLINE
                )

                onLinkFound(subtitle)
            }
        }
    }

    private suspend fun processSources(
        sources: String?,
        qualities: String?,
        file: ExternalResponse.Data.StreamFile,
        onLinkFound: (MediaLink) -> Unit
    ) {
        listOf(sources, qualities).forEach topForEach@ { item ->
            if (item == null) return@topForEach

            fromJson<List<ExternalSources>>(item).forEach { source ->
                if (!source.label.equals("AUTO", true) && !source.type.equals(
                        "video/mp4",
                        true
                    )
                ) return@forEach

                val url = (source.hlsUrl ?: source.file)?.replace("\\/", "/") ?: return@forEach

                onLinkFound(
                    Stream(
                        name = """
                            =< SuperStream >=
                            Quality 👁️: ${source.label}
                            File 📹: ${file.fileName}
                            Size 💾: ${file.fileSize}
                            Date ⌚: ${file.addedOn}
                            =================
                        """.trimIndent(),
                        url = url
                    )
                )
            }
        }
    }

    private fun getEpisodeSlug(season: Int? = null, episode: Int? = null): Pair<String, String> {
        if (season == null && episode == null) {
            return "" to ""
        }

        val seasonSlug = season?.let { if (it < 10) "0$it" else "$it" } ?: ""
        val episodeSlug = episode?.let { if (it < 10) "0$it" else "$it" } ?: ""

        return seasonSlug to episodeSlug
    }
}