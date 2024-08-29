package com.flxProviders.superstream.api

import android.content.Context
import com.flixclusive.core.util.common.dispatcher.AppDispatchers.Companion.withMainContext
import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.coroutines.mapIndexedAsync
import com.flixclusive.core.util.film.filter.FilterList
import com.flixclusive.core.util.network.WebViewInterceptor.Companion.addWebViewInterceptor
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
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
import com.flxProviders.superstream.api.dto.ExternalResponse
import com.flxProviders.superstream.api.dto.ExternalSources
import com.flxProviders.superstream.api.dto.ITEMS_PER_PAGE
import com.flxProviders.superstream.api.dto.SearchData
import com.flxProviders.superstream.api.dto.SearchData.Companion.toSearchResponseData
import com.flxProviders.superstream.api.settings.TOKEN_KEY
import com.flxProviders.superstream.api.util.CloudflareWebViewInterceptor
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import java.util.Locale
import kotlin.random.Random

class SuperStreamApi(
    client: OkHttpClient,
    provider: Provider,
    context: Context,
    private val settings: ProviderSettings
) : ProviderApi(client, context, provider) {
    private val token: String?
        get() = settings.getString(TOKEN_KEY, null)

    private val tokenHeaders: Map<String, String>
        get() = mapOf("Cookie" to "ui=$token")

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
        val query = imdbId ?: title
        val apiQuery = String.format(
            Locale.ROOT,
            BuildConfig.SUPERSTREAM_THIRD_API,
            query,
            page,
            ITEMS_PER_PAGE,
            Random.nextInt(0, Int.MAX_VALUE)
        )

        val response = client.request(apiQuery).execute()
            .fromJson<SearchData>("[${provider.name}] Couldn't search for $query")

        return response.toSearchResponseData(provider.name)
    }

    override suspend fun getFilmDetails(film: Film): FilmDetails {
        throw NotImplementedError("Not yet implemented. Please come back soon for future updates.")
    }

    private suspend fun getSourceLinksFromFourthApi(
        watchId: String,
        filmType: BoxType,
        episode: Int?,
        season: Int?,
        onLinkFound: (MediaLink) -> Unit
    ) {
        val shareKey = fetchShareKey(watchId, filmType)
        val sharedResources = fetchSharedResources(shareKey)
        val files = fetchFiles(sharedResources, season, episode, shareKey)
        val cleanedFiles = cleanFiles(files, shareKey)

        processFiles(cleanedFiles, shareKey, onLinkFound)
    }

    private suspend fun fetchShareKey(watchId: String, filmType: BoxType): String {
        val interceptor = withMainContext {
            CloudflareWebViewInterceptor(context)
        }

        val url =
            "${BuildConfig.SUPERSTREAM_FIRST_API}/index/share_link?id=$watchId&type=${filmType.value}"
        return client
            .addWebViewInterceptor(interceptor)
            .request(url)
            .execute()
            .use { response ->
                val string = response.body?.string()
                requireNotNull(string) { "[${provider.name}] Failed to fetch share key." }
                fromJson<ExternalResponse>(string)
                    .data?.link?.substringAfterLast("/")
                    ?: throw Exception("[${provider.name}] No share key found.")
            }
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
    ): List<ExternalResponse.Data.FileList> {
        return if (season == null) {
            sharedResources.fileList
        } else {
            fetchSeasonFiles(sharedResources, season, episode!!, shareKey)
        } ?: throw Exception("[${provider.name}] No FIDs found.")
    }

    private fun fetchSeasonFiles(
        sharedResources: ExternalResponse.Data,
        season: Int,
        episode: Int,
        shareKey: String
    ): List<ExternalResponse.Data.FileList>? {
        val parentId = findSeasonParentId(sharedResources, season, shareKey)
        val url =
            "${BuildConfig.SUPERSTREAM_SECOND_API}/file/file_share_list?share_key=$shareKey&parent_id=$parentId&page=1"
        val episodesShareRes = client.request(url)
            .execute()
            .fromJson<ExternalResponse>("[${provider.name}] Failed to fetch share key.")
            .data ?: throw Exception("[${provider.name}] No shared resources found (Stage 2).")

        val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
        return episodesShareRes.fileList?.filter {
            it.fileName?.contains("s${seasonSlug}e${episodeSlug}", true) == true
        }
    }

    private fun findSeasonParentId(
        sharedResources: ExternalResponse.Data,
        season: Int,
        shareKey: String
    ): Long {
        var parentId = sharedResources.fileList
            ?.find { it.fileName.equals("season $season", true) }?.fid

        if (parentId == null && sharedResources.fileList?.isNotEmpty() == true) {
            val subDirectoryId = sharedResources.fileList.first().fid
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
            .data?.fileList
            ?.find { it.fileName.equals("season $season", true) }?.fid
    }

    private suspend fun cleanFiles(
        files: List<ExternalResponse.Data.FileList>,
        shareKey: String
    ): List<ExternalResponse.Data.FileList> {
        val cleanedFiles = mutableListOf<ExternalResponse.Data.FileList>()
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
    ): List<ExternalResponse.Data.FileList> {
        val url =
            "${BuildConfig.SUPERSTREAM_SECOND_API}/file/file_share_list?share_key=$shareKey&parent_id=$fid"
        val humanizedHeaders = mapOf("Accept-Language" to "en")
        return client.request(url, headers = humanizedHeaders.toHeaders())
            .execute()
            .fromJson<ExternalResponse>("[${provider.name}] Failed to fetch directory files.")
            .data?.fileList
            ?: throw Exception("[${provider.name}] No files found on this directory.")
    }

    private suspend fun processFiles(
        cleanedFiles: List<ExternalResponse.Data.FileList>,
        shareKey: String,
        onLinkFound: (MediaLink) -> Unit
    ) {
        cleanedFiles.mapAsync { fileList ->
            val playerUrl =
                "${BuildConfig.SUPERSTREAM_SECOND_API}/file/player?fid=${fileList.fid}&share_key=$shareKey"
            val player = fetchPlayerContent(playerUrl)
            val (subtitles, sources, qualities) = extractPlayerData(player)

            asyncCalls(
                { processSubtitles(subtitles, onLinkFound) },
                { processSources(sources, qualities, onLinkFound) }
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

    private suspend fun processSubtitles(subtitles: String?, onLinkFound: (MediaLink) -> Unit) {
        if (subtitles == null) return

        val jsoupObject = Jsoup.parse(subtitles)
        val div = jsoupObject.selectFirst("div.right2") ?: return
        val languages = div.select("ul")

        languages.mapAsync { languageElement ->
            val language = languageElement.id()
            val subtitleLinks = languageElement.select("li")

            subtitleLinks.mapIndexedAsync { _, subtitleLink ->
                val link = subtitleLink.attr("data-url")
                onLinkFound(
                    Subtitle(
                        language = language,
                        url = link,
                        type = SubtitleSource.ONLINE
                    )
                )
            }
        }
    }

    private suspend fun processSources(
        sources: String?,
        qualities: String?,
        onLinkFound: (MediaLink) -> Unit
    ) {
        listOf(sources, qualities).mapAsync { item ->
            if (item == null) return@mapAsync

            fromJson<List<ExternalSources>>(item).forEach { source ->
                if (!source.label.equals("AUTO", true) && !source.type.equals(
                        "video/mp4",
                        true
                    )
                ) return@forEach

                val url = (source.hlsUrl ?: source.file)?.replace("\\/", "/") ?: return@forEach

                onLinkFound(
                    Stream(
                        name = "[${provider.name}]> ${source.label}",
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