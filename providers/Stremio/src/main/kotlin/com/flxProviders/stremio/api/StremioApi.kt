@file:Suppress("SpellCheckingInspection")

package com.flxProviders.stremio.api

import android.content.Context
import androidx.compose.ui.util.fastForEach
import com.flixclusive.core.ui.common.util.showToast
import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.ioLaunch
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.log.debugLog
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.SearchResponseData
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.settings.ProviderSettingsManager
import com.flxProviders.stremio.api.model.Addon
import com.flxProviders.stremio.api.model.Catalog
import com.flxProviders.stremio.api.model.FetchCatalogResponse
import com.flxProviders.stremio.api.model.FetchMetaResponse
import com.flxProviders.stremio.api.model.Stream.Companion.toSourceLink
import com.flxProviders.stremio.api.model.StreamResponse
import com.flxProviders.stremio.api.model.toFilmDetails
import com.flxProviders.stremio.api.model.toFilmSearchItem
import com.flxProviders.stremio.api.util.OpenSubtitlesUtil.fetchSubtitles
import com.flxProviders.stremio.api.util.isValidUrl
import com.flxProviders.stremio.settings.util.AddonUtil.DEFAULT_META_PROVIDER
import com.flxProviders.stremio.settings.util.AddonUtil.DEFAULT_META_PROVIDER_BASE_URL
import com.flxProviders.stremio.settings.util.AddonUtil.downloadAddon
import com.flxProviders.stremio.settings.util.AddonUtil.getAddons
import com.flxProviders.stremio.settings.util.AddonUtil.toProviderCatalog
import com.flxProviders.stremio.settings.util.AddonUtil.updateAddon
import com.flxProviders.stremio.settings.util.Success
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

internal const val STREAMIO_ADDONS_KEY = "streamio_addons"
internal const val ADDON_SOURCE_KEY = "addonSource"
internal const val ADDONS_MIGRATION_KEY = "addons_has_been_migrated"
internal const val MEDIA_TYPE_KEY = "type"
internal const val STREMIO = "Stremio"

internal class StremioApi(
    context: Context,
    client: OkHttpClient,
    private val settings: ProviderSettingsManager
) : ProviderApi(client) {
    init {
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
                context.showToast("""
                    $successMigrations out of ${addons.size} Stremio addons have been migrated successfully addons.
                    """.trimIndent()
                )
            }

            if (successMigrations == addons.size - 1) {
                settings.setBool(ADDONS_MIGRATION_KEY, true)
            }
        }
    }

    override val name: String
        get() = STREMIO

    override val catalogs: List<ProviderCatalog>
        get() = safeCall {
            settings.getAddons().flatMap { addon ->
                addon.catalogs?.map { catalog ->
                    catalog.toProviderCatalog()
                } ?: emptyList()
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

    override suspend fun getSourceLinks(
        watchId: String,
        film: FilmDetails,
        episode: Episode?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
        asyncCalls(
            {
                film.getLinks(
                    watchId = watchId,
                    episode = episode,
                    onLinkLoaded = onLinkLoaded,
                    onSubtitleLoaded = onSubtitleLoaded
                )
            },
            {
                client.fetchSubtitles(
                    imdbId = film.imdbId ?: film.identifier,
                    season = episode?.season,
                    episode = episode?.number,
                    onSubtitleLoaded = onSubtitleLoaded
                )
            },
        )
    }

    override suspend fun search(
        title: String,
        page: Int,
        id: String?,
        imdbId: String?,
        tmdbId: Int?
    ): SearchResponseData<FilmSearchItem> {
        // TODO("Add catalog system")

        return SearchResponseData(
            results = listOf(
                FilmSearchItem(
                    id = id ?: tmdbId?.toString() ?: imdbId!!,
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
        episode: Episode?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
        val addons = settings.getAddons()
        val streamType = customProperties[MEDIA_TYPE_KEY]
        val addonSourceName = customProperties[ADDON_SOURCE_KEY]
        val isFromStremio = providerName.equals(STREMIO, true)
        val id = if (isFromStremio) watchId else imdbId ?: return

        if (addonSourceName != null) {
            val addonSource = getAddonByName(name = addonSourceName)
            if (addonSource.hasStream) {
                var linksLoaded = 0
                addonSource.getStream(
                    watchId = id,
                    filmType = filmType,
                    streamType = streamType,
                    episode = episode,
                    isFromStremio = isFromStremio,
                    onLinkLoaded = {
                        linksLoaded++
                        onLinkLoaded(it)
                    },
                    onSubtitleLoaded = onSubtitleLoaded
                )

                if (linksLoaded > 0)
                    return
            }
        }

        addons.mapAsync { addon ->
            if (!addon.hasStream)
                return@mapAsync

            addon.getStream(
                watchId = id,
                filmType = filmType,
                streamType = streamType,
                episode = episode,
                isFromStremio = isFromStremio,
                onLinkLoaded = onLinkLoaded,
                onSubtitleLoaded = onSubtitleLoaded
            )
        }
    }

    private fun Addon.getStream(
        watchId: String,
        filmType: FilmType,
        isFromStremio: Boolean,
        streamType: String?,
        episode: Episode?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
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

        debugLog("$baseUrl/$query")
        val response = client.request(
            url = "$baseUrl/$query"
        ).execute()
            .fromJson<StreamResponse>()

        debugLog(response)

        if (response.err != null)
            return

        response.streams.forEach { stream ->
            val sourceLink = stream.toSourceLink()
            if (sourceLink != null)
                onLinkLoaded(sourceLink)

            stream.subtitles?.forEach sub@ { subtitle ->
                val isValidUrl = isValidUrl(subtitle.url)
                if (!isValidUrl) return@sub

                onSubtitleLoaded(subtitle)
            }
        }
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

    private fun Catalog.getCatalogQuery(
        page: Int = 1,
        searchQuery: String? = null
    ): String {
        var query = "catalog/$type/$id"

        if (page > 1 && canPaginate) {
            query += "/skip=${page * (pageSize ?: 20)}"
        }

        if (searchQuery != null) {
            query += "/search=$searchQuery"
        }

        return "$query.json"
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
}