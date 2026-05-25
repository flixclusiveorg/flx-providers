package com.flixclusive.provider.app.stremio.feature.catalog

import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastMap
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.model.provider.Catalog
import com.flixclusive.provider.app.stremio.StremioPlugin
import com.flixclusive.provider.app.stremio.core.model.FetchCatalogResponse
import com.flixclusive.provider.app.stremio.core.model.StremioCatalog
import com.flixclusive.provider.app.stremio.core.model.toPartialMedia
import com.flixclusive.provider.app.stremio.core.util.AddonUtil.getAddon
import com.flixclusive.provider.app.stremio.core.util.AddonUtil.getAddons
import com.flixclusive.provider.capability.CatalogProviderApi
import okhttp3.OkHttpClient

class StremioCatalogProvider internal constructor(
    private val client: OkHttpClient,
    private val plugin: StremioPlugin,
): CatalogProviderApi {
    override suspend fun getCatalogItems(
        catalog: Catalog,
        page: Int
    ): PaginatedMedia<PartialMedia> {
        val catalogProperties = fromJson<StremioCatalog>(catalog.url)
        val addonId = catalogProperties.addonId
        val addon = plugin.settings.getAddon(id = addonId)

        val query = catalogProperties.getCatalogQuery(page = page)

        val failedFetchErrorMessage = "[${catalog.name}]> Coudn't fetch catalog items"
        val response = client.request(url = "${addon.baseUrl}/$query")
            .execute()
            .fromJson<FetchCatalogResponse>(errorMessage = failedFetchErrorMessage)

        if (response.err != null) {
            throw Exception(failedFetchErrorMessage)
        }

        val results = response.items?.fastMap {
            it.toPartialMedia(
                addonId = addon.id,
                providerId = plugin.id,
            )
        } ?: emptyList()

        return PaginatedMedia(
            page = page,
            results = results,
            hasNextPage = results.isNotEmpty(),
        )
    }

    override suspend fun getCatalogs(): List<Catalog> {
        return plugin.settings.getAddons().fastFlatMap { addon ->
            addon.getHomeCatalogs(plugin.id)
        }
    }
}