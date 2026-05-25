package com.flixclusive.provider.app.stremio.feature.search

import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.provider.ProviderPlugin
import com.flixclusive.provider.app.stremio.core.model.Addon
import com.flixclusive.provider.app.stremio.core.model.FetchCatalogResponse
import com.flixclusive.provider.app.stremio.core.model.toPartialMedia
import com.flixclusive.provider.app.stremio.core.util.AddonUtil
import com.flixclusive.provider.app.stremio.core.util.AddonUtil.downloadAddon
import com.flixclusive.provider.app.stremio.core.util.AddonUtil.getAddons
import com.flixclusive.provider.capability.SearchProviderApi
import com.flixclusive.provider.filter.FilterList
import okhttp3.OkHttpClient

class StremioSearchProvider internal constructor(
    private val client: OkHttpClient,
    private val plugin: ProviderPlugin,
) : SearchProviderApi {
    private var cinemataAddon: Addon? = null

    override val filters: FilterList = FilterList()

    override suspend fun search(
        query: String,
        page: Int,
        filters: FilterList
    ): PaginatedMedia<PartialMedia> {
        val results = mutableSetOf<PartialMedia>()
        if (cinemataAddon == null) {
            cinemataAddon = client.downloadAddon(
                url = AddonUtil.DEFAULT_META_PROVIDER_BASE_URL,
            )
        }

        (listOfNotNull(cinemataAddon) + plugin.settings.getAddons()).mapAsync { addon ->
            addon.searchableCatalogs.mapAsync { catalog ->
                val query = catalog.getCatalogQuery(
                    searchQuery = query,
                    page = page,
                )

                val items = safeCall {
                    val baseUrl = when (catalog.addonId) {
                        AddonUtil.DEFAULT_META_PROVIDER_ID -> AddonUtil.DEFAULT_META_PROVIDER_BASE_URL
                        else -> addon.baseUrl
                    }

                    client.request(url = "$baseUrl/$query")
                        .execute()
                        .fromJson<FetchCatalogResponse>()
                        .items?.mapAsync {
                            it.toPartialMedia(
                                addonId = catalog.addonId,
                                providerId = plugin.id,
                            )
                        }
                } ?: emptyList()

                results.addAll(items)
            }
        }

        return PaginatedMedia(
            results = results.toList(),
            page = page,
            hasNextPage = results.size >= 20
        )
    }
}