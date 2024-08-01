@file:Suppress("SpellCheckingInspection")

package com.flxProviders.stremio.api

import android.content.Context
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastForEach
import com.flixclusive.core.ui.common.util.showToast
import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.ioLaunch
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.film.filter.FilterList
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.MediaLink
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.model.provider.Stream
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.SearchResponseData
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.settings.ProviderSettings
import com.flxProviders.stremio.api.model.Addon
import com.flxProviders.stremio.api.model.Catalog
import com.flxProviders.stremio.api.model.EMBEDDED_IMDB_ID_KEY
import com.flxProviders.stremio.api.model.EMBEDDED_STREAM_KEY
import com.flxProviders.stremio.api.model.FetchCatalogResponse
import com.flxProviders.stremio.api.model.FetchMetaResponse
import com.flxProviders.stremio.api.model.StreamDto.Companion.toStreamLink
import com.flxProviders.stremio.api.model.StreamResponse
import com.flxProviders.stremio.api.model.toFilmDetails
import com.flxProviders.stremio.api.model.toFilmSearchItem
import com.flxProviders.stremio.api.util.OpenSubtitlesUtil.fetchSubtitles
import com.flxProviders.stremio.api.util.isValidUrl
import com.flxProviders.stremio.settings.util.AddonUtil.DEFAULT_META_PROVIDER
import com.flxProviders.stremio.settings.util.AddonUtil.DEFAULT_META_PROVIDER_BASE_URL
import com.flxProviders.stremio.settings.util.AddonUtil.downloadAddon
import com.flxProviders.stremio.settings.util.AddonUtil.getAddons
import com.flxProviders.stremio.settings.util.AddonUtil.updateAddon
import com.flxProviders.stremio.settings.util.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

internal const val STREAMIO_ADDONS_KEY = "streamio_addons"
internal const val ADDON_SOURCE_KEY = "addonSource"
internal const val ADDONS_MIGRATION_KEY = "addons_has_been_migrated_1" // Just increment by 1
internal const val MEDIA_TYPE_KEY = "type"
internal const val STREMIO = "Stremio"

internal class StremioApi(
    context: Context,
    provider: Provider,
    client: OkHttpClient,
    private val settings: ProviderSettings
) : ProviderApi(client, provider) {
    init {
        context.migrateOldAddons()
    }

    private val name = STREMIO

    override val catalogs: List<ProviderCatalog>
        get() = safeCall {
            settings.getAddons().fastFlatMap { addon ->
                addon.homeCatalogs
            }
        } ?: emptyList()

    override suspend fun getCatalogItems(
        catalog: ProviderCatalog,
        page: Int
    ): SearchResponseData<FilmSearchItem> {
        val catalogProperties = fromJson<Catalog>(catalog.url)
        val addonSource = catalogProperties.addonSource
        val addon = getAddonByName(name = addonSource)

        val query = catalogProperties.getCatalogQuery(page = page)

        val failedFetchErrorMessage = "[${catalog.name}]> Coudn't fetch catalog items"
        val response = client.request(url = "${addon.baseUrl}/$query")
            .execute()
            .fromJson<FetchCatalogResponse>(errorMessage = failedFetchErrorMessage)

        if (response.err != null) {
            throw Exception(failedFetchErrorMessage)
        }

        val results = mutableListOf<FilmSearchItem>()
        response.items?.fastForEach {
            val item = it.toFilmSearchItem(addonName = addon.name)
            results.add(item)
        }

        return SearchResponseData(
            page = page,
            results = results,
            hasNextPage = results.size != 0,
        )
    }

    override suspend fun getLinks(
        watchId: String,
        film: FilmDetails,
        episode: Episode?
    ): List<MediaLink> {
        val links = mutableListOf<MediaLink>()
        
        asyncCalls(
            {
                val streamLinks = film.getLinks(
                    watchId = watchId,
                    episode = episode
                )
                
                links.addAll(streamLinks)
            },
            {
                val imdbId = film.customProperties[EMBEDDED_IMDB_ID_KEY]
                    ?: film.imdbId
                    ?: film.identifier

                val subtitles = client.fetchSubtitles(
                    imdbId = imdbId,
                    season = episode?.season,
                    episode = episode?.number
                )

                links.addAll(subtitles)
            },
        )
        
        return links
    }

    override suspend fun search(
        title: String,
        page: Int,
        id: String?,
        imdbId: String?,
        tmdbId: Int?,
        filters: FilterList,
    ): SearchResponseData<FilmSearchItem> {
        val identifier = id ?: tmdbId?.toString() ?: imdbId
        if (identifier != null) {
            return SearchResponseData(
                results = listOf(
                    FilmSearchItem(
                        id = identifier,
                        title = title,
                        providerName = name,
                        posterImage = null,
                        backdropImage = null,
                        homePage = null,
                        filmType = FilmType.MOVIE
                    )
                )
            )
        }

        val results = mutableListOf<FilmSearchItem>()
        settings.getAddons().mapAsync { addon ->
            addon.searchableCatalogs.mapAsync { catalog ->
                val query = catalog.getCatalogQuery(
                    searchQuery = title,
                    page = page,
                )

                val items = safeCall {
                    val baseUrl = when (catalog.addonSource) {
                        DEFAULT_META_PROVIDER -> DEFAULT_META_PROVIDER_BASE_URL
                        else -> addon.baseUrl
                    }

                    client.request(url = "$baseUrl/$query")
                        .execute()
                        .fromJson<FetchCatalogResponse>()
                        .items?.mapAsync {
                            it.toFilmSearchItem(addonName = addon.name)
                        }
                } ?: emptyList()

                results.addAll(items)
            }
        }

        return SearchResponseData(
            results = results.fastDistinctBy { it.identifier }.toList(),
            page = page,
            hasNextPage = results.size >= 20
        )
    }

    override suspend fun getFilmDetails(film: Film): FilmDetails {
        val nameKey = "${film.id}=${film.title}"

        val addonSource = film.customProperties[ADDON_SOURCE_KEY]
            ?: throw IllegalArgumentException("[$nameKey]> Addon source must not be null!")
        val type = film.customProperties[MEDIA_TYPE_KEY]
            ?: throw IllegalArgumentException("[$nameKey]> Media type must not be null!")

        val addon = getAddonByName(name = addonSource)
        if (!addon.hasMeta && film.hasImdbId) {
            return getFilmDetailsFromDefaultMetaProvider(
                id = film.imdbId!!,
                type = type,
                nameKey = nameKey
            )
        } else if (!addon.hasMeta)
            throw IllegalArgumentException("[${addonSource}]> Addon has no metadata resource!")

        val url = "${addon.baseUrl}/meta/$type/${film.identifier}.json"
        return getFilmDetails(
            addonName = addonSource,
            nameKey = nameKey,
            url = url
        )
    }

    private fun getFilmDetails(
        addonName: String,
        nameKey: String,
        url: String
    ): FilmDetails {
        val failedToFetchMetaDataMessage = "[${addonName}]> Coudn't fetch meta data of $nameKey ($url)"
        val response = client.request(url = url).execute()
            .fromJson<FetchMetaResponse>(errorMessage = failedToFetchMetaDataMessage)

        if (response.err != null) {
            throw Exception(failedToFetchMetaDataMessage)
        }

        return response.film?.toFilmDetails(addonName)
            ?: throw IllegalArgumentException("[$nameKey]> Meta data is null!")
    }

    private suspend fun FilmDetails.getLinks(
        watchId: String,
        episode: Episode?
    ): List<MediaLink> {
        val embeddedStream = customProperties[EMBEDDED_STREAM_KEY]
        if (embeddedStream != null) {
            return listOf(
                Stream(
                    name = title,
                    url = embeddedStream
                )
            )
        }

        val addons = settings.getAddons()
        val streamType = customProperties[MEDIA_TYPE_KEY]
        val addonSourceName = customProperties[ADDON_SOURCE_KEY]
        val isFromStremio = providerName.equals(STREMIO, true)
        val id = if (isFromStremio) watchId else imdbId
            ?: throw IllegalArgumentException("[$id]> IMDB ID should not be null!")

        if (addonSourceName != null) {
            val addonSource = getAddonByName(name = addonSourceName)
            if (addonSource.hasStream) {
                val streams = addonSource.getStream(
                    watchId = id,
                    filmType = filmType,
                    streamType = streamType,
                    episode = episode,
                    isFromStremio = isFromStremio
                )

                if (streams.isNotEmpty())
                    return streams
            }
        }

        val links = mutableListOf<MediaLink>()
        addons.mapAsync { addon ->
            if (!addon.hasStream)
                return@mapAsync

            val streams = addon.getStream(
                watchId = id,
                filmType = filmType,
                streamType = streamType,
                episode = episode,
                isFromStremio = isFromStremio
            )

            links.addAll(streams)
        }

        return links
    }

    private fun Addon.getStream(
        watchId: String,
        filmType: FilmType,
        isFromStremio: Boolean,
        streamType: String?,
        episode: Episode?
    ): List<MediaLink> {
        val hasStreamUrlOnEpisode = episode != null && isValidUrl(episode.id)
        if (hasStreamUrlOnEpisode) {
            return listOf(
                Stream(
                    url = episode!!.id,
                    name = episode.title,
                )
            )
        }

        val query = when (streamType) {
            null -> getStreamQuery(
                id = watchId,
                type = filmType,
                isFromStremio = isFromStremio,
                episode = episode
            )
            else -> getStreamQuery(
                id = watchId,
                type = streamType,
                isFromStremio = isFromStremio,
                episode = episode
            )
        }

        val response = client.request(
            url = "$baseUrl/$query"
        ).execute()
            .fromJson<StreamResponse>()

        if (response.err != null)
            return emptyList()

        val links = mutableListOf<MediaLink>()
        response.streams.forEach { stream ->
            val sourceLink = stream.toStreamLink()
            if (sourceLink != null)
                links.add(sourceLink)

            stream.subtitles?.forEach sub@ { subtitle ->
                val isValidUrl = isValidUrl(subtitle.url)
                if (!isValidUrl) return@sub

                links.add(subtitle)
            }
        }

        return links
    }

    private fun getFilmDetailsFromDefaultMetaProvider(
        id: String,
        type: String,
        nameKey: String
    ): FilmDetails {
        val url = "$DEFAULT_META_PROVIDER_BASE_URL/meta/$type/$id.json"

        return getFilmDetails(
            addonName = DEFAULT_META_PROVIDER,
            nameKey = nameKey,
            url = url
        )
    }

    private fun getAddonByName(name: String): Addon {
        return settings.getAddons()
            .firstOrNull { it.isCorrectSource(name) }
            ?: throw IllegalArgumentException("[${name}]> Addon cannot be found")
    }

    private fun Addon.isCorrectSource(name: String)
            = this.name.equals(name, true)

    private val Film.hasImdbId: Boolean
        get() = imdbId != null || id?.startsWith("tt") == true

    private fun Context.migrateOldAddons() {
        ioLaunch {
            val isSet = settings.getBool(ADDONS_MIGRATION_KEY, false)
            if (!isSet) {
                return@ioLaunch
            }

            var successMigrations = 0
            val addons = settings.getAddons()
            addons.forEach { addon ->
                val url = addon.baseUrl ?: return@ioLaunch
                val updatedAddon = client.downloadAddon(url = url) ?: return@forEach

                val response = settings.updateAddon(addon = updatedAddon)

                if (response is Success) {
                    successMigrations++
                }
            }

            withContext(Dispatchers.Main) {
                showToast("""
                    $successMigrations out of ${addons.size} Stremio addons have been migrated successfully addons.
                    """.trimIndent()
                )
            }

            if (successMigrations == addons.size - 1) {
                settings.setBool(ADDONS_MIGRATION_KEY, true)
            }
        }
    }
}