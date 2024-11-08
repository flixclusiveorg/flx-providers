@file:Suppress("SpellCheckingInspection")

package com.flxProviders.stremio.api

import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastForEach
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withDefaultContext
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmDetails
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.filter.FilterList
import com.flixclusive.provider.settings.ProviderSettings
import com.flxProviders.stremio.api.model.Addon
import com.flxProviders.stremio.api.model.Catalog
import com.flxProviders.stremio.api.model.EMBEDDED_IMDB_ID_KEY
import com.flxProviders.stremio.api.model.EMBEDDED_STREAM_KEY
import com.flxProviders.stremio.api.model.FetchCatalogResponse
import com.flxProviders.stremio.api.model.FetchMetaResponse
import com.flxProviders.stremio.api.model.StreamResponse
import com.flxProviders.stremio.api.model.SubtitleResponse
import com.flxProviders.stremio.api.model.toFilmDetails
import com.flxProviders.stremio.api.model.toFilmSearchItem
import com.flxProviders.stremio.api.util.isValidUrl
import com.flxProviders.stremio.settings.util.AddonUtil.DEFAULT_META_PROVIDER
import com.flxProviders.stremio.settings.util.AddonUtil.DEFAULT_META_PROVIDER_BASE_URL
import com.flxProviders.stremio.settings.util.AddonUtil.getAddons
import okhttp3.OkHttpClient

internal const val STREAMIO_ADDONS_KEY = "streamio_addons"
internal const val ADDON_SOURCE_KEY = "addonSource"
internal const val MEDIA_TYPE_KEY = "type"
internal const val STREMIO = "Stremio"

internal class StremioApi(
    provider: Provider,
    client: OkHttpClient,
    private val settings: ProviderSettings
) : ProviderApi(
    client = client,
    provider = provider
) {
    private val name = STREMIO

    override val testFilm: FilmDetails
        get() = (super.testFilm as Movie).copy(
            id = super.testFilm.imdbId,
            providerName = name,
        )

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
        episode: Episode?,
        onLinkFound: (MediaLink) -> Unit
    ) {
        withDefaultContext {
            film.getLinks(
                watchId = watchId,
                episode = episode,
                onLinkFound = onLinkFound
            )
        }
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
                            it.toFilmSearchItem(addonName = catalog.addonSource)
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
        val actualId = film.imdbId ?: film.id

        val addonSource = film.customProperties[ADDON_SOURCE_KEY]
            ?: throw IllegalArgumentException("[$nameKey]> Addon source must not be null!")
        val type = film.customProperties[MEDIA_TYPE_KEY]
            ?: throw IllegalArgumentException("[$nameKey]> Media type must not be null!")

        if (addonSource == DEFAULT_META_PROVIDER) {
            return getFilmDetailsFromDefaultMetaProvider(
                id = actualId,
                type = type,
                nameKey = nameKey
            )
        }

        val addon = getAddonByName(name = addonSource)
        require(addon.hasMeta || film.hasImdbId) {
            "[$nameKey]> Addon has no metadata resource!"
        }

        if (!addon.hasMeta && film.hasImdbId) {
            return getFilmDetailsFromDefaultMetaProvider(
                id = actualId,
                type = type,
                nameKey = nameKey
            )
        }

        val url = "${addon.baseUrl}/meta/$type/${film.identifier}.json"
        return getFilmDetails(
            addonName = addonSource,
            nameKey = nameKey,
            url = url
        )
    }

    private suspend fun getFilmDetails(
        addonName: String,
        nameKey: String,
        url: String
    ): FilmDetails {
        val failedToFetchMetaDataMessage = "[${addonName}]> Coudn't fetch meta data of $nameKey ($url)"
        val response = withIOContext {
            client.request(url = url).execute()
                .fromJson<FetchMetaResponse>(errorMessage = failedToFetchMetaDataMessage)
        }

        if (response.err != null) {
            throw Exception(failedToFetchMetaDataMessage)
        }

        return response.film?.toFilmDetails(addonName)
            ?: throw IllegalArgumentException("[$nameKey]> Meta data is null!")
    }

    private suspend fun FilmDetails.getLinks(
        watchId: String,
        episode: Episode?,
        onLinkFound: (MediaLink) -> Unit
    ) {
        val embeddedStream = customProperties[EMBEDDED_STREAM_KEY]
        if (embeddedStream != null) {
            val stream = Stream(
                name = title,
                url = embeddedStream
            )

            return onLinkFound(stream)
        }

        val addons = settings.getAddons()
        val streamType = customProperties[MEDIA_TYPE_KEY]
        val addonSourceName = customProperties[ADDON_SOURCE_KEY]
        val isFromStremio = providerName.equals(STREMIO, true)
        val id = if (isFromStremio) watchId else imdbId
            ?: throw IllegalArgumentException("[$id]> IMDB ID should not be null!")

        if (addonSourceName != null && addonSourceName != DEFAULT_META_PROVIDER) {
            val addonSource = getAddonByName(name = addonSourceName)
            if (addonSource.hasStream) {
                addonSource.getStream(
                    watchId = id,
                    filmType = filmType,
                    streamType = streamType,
                    episode = episode,
                    isFromStremio = isFromStremio,
                    onLinkFound = onLinkFound
                )
            }
        }

        val mediaIdForSubtitles = customProperties[EMBEDDED_IMDB_ID_KEY]
            ?: imdbId
            ?: id

        addons.forEach { addon ->
            if (addon.hasStream) {
                addon.getStream(
                    watchId = id,
                    filmType = filmType,
                    streamType = streamType,
                    episode = episode,
                    isFromStremio = isFromStremio,
                    onLinkFound = onLinkFound
                )
            }

            if (addon.hasSubtitle) {
                addon.getSubtitle(
                    watchId = mediaIdForSubtitles,
                    episode = episode,
                    onLinkFound = onLinkFound
                )
            }
        }
    }

    private suspend fun Addon.getSubtitle(
        watchId: String,
        episode: Episode?,
        onLinkFound: (MediaLink) -> Unit
    ) {
        val slug = if(episode == null) {
            "subtitles/movie/$watchId"
        } else {
            "subtitles/series/$watchId:${episode.season}:${episode.number}"
        }

        val response = withIOContext {
            client.request(
                url = "$baseUrl/$slug.json"
            ).execute()
        }.fromJson<SubtitleResponse>()

        if (response.err != null)
            return

        response.subtitles.forEach { subtitle ->
            val isValidUrl = isValidUrl(subtitle.url)
            if (!isValidUrl) return@forEach

            onLinkFound(
                subtitle.toSubtitle(addonName = this@getSubtitle.name)
            )
        }
    }

    private suspend fun Addon.getStream(
        watchId: String,
        filmType: FilmType,
        isFromStremio: Boolean,
        streamType: String?,
        episode: Episode?,
        onLinkFound: (MediaLink) -> Unit
    ) {
        val hasStreamUrlOnEpisode = episode != null && isValidUrl(episode.id)
        if (hasStreamUrlOnEpisode) {
            val stream = Stream(
                name = episode!!.title,
                url = episode.id
            )

            return onLinkFound(stream)
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

        val response = withIOContext {
            client.request(
                url = "$baseUrl/$query"
            ).execute()
        }.fromJson<StreamResponse>()

        if (response.err != null)
            return

        response.streams.forEach { stream ->
            val streamLink = stream.toStreamLink()
            if (streamLink != null)
                onLinkFound(streamLink)

            stream.subtitles?.forEach sub@ { subtitle ->
                val isValidUrl = isValidUrl(subtitle.url)
                if (!isValidUrl) return@sub

                onLinkFound(subtitle)
            }
        }
    }

    private suspend fun getFilmDetailsFromDefaultMetaProvider(
        id: String?,
        type: String,
        nameKey: String
    ): FilmDetails {
        require(id != null) {
            "[$nameKey]> IMDB ID must not be null!"
        }

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
}