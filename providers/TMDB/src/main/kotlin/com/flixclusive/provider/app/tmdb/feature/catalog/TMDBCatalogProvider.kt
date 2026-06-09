package com.flixclusive.provider.app.tmdb.feature.catalog

import android.content.Context
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.model.provider.Catalog
import com.flixclusive.provider.app.tmdb.core.config.KEY_CATALOGS
import com.flixclusive.provider.app.tmdb.core.config.readImageConfig
import com.flixclusive.provider.app.tmdb.core.model.UserCatalog
import com.flixclusive.provider.app.tmdb.core.model.dto.FilmSearchItemDto
import com.flixclusive.provider.app.tmdb.core.model.dto.SearchPageDto
import com.flixclusive.provider.app.tmdb.core.model.loadCatalogSeed
import com.flixclusive.provider.capability.CatalogProviderApi
import com.flixclusive.provider.extensions.getObject
import com.flixclusive.provider.extensions.setObject
import okhttp3.OkHttpClient

internal class TMDBCatalogProvider(
    private val client: OkHttpClient,
    private val settings: DataStore<Preferences>,
    private val context: Context,
    private val providerId: String,
) : CatalogProviderApi {

    override suspend fun getCatalogs(): List<Catalog> {
        val userCatalogs = settings.getObject<List<UserCatalog>>(KEY_CATALOGS)
        if (userCatalogs == null) {
            val seeded = loadCatalogSeed(context)
            settings.setObject(KEY_CATALOGS, seeded)
            return seeded.filter { it.enabled }.fastMap { it.toCatalog(providerId) }
        }
        return userCatalogs.filter { it.enabled }.fastMap { it.toCatalog(providerId) }
    }

    override suspend fun getCatalogItems(catalog: Catalog, page: Int): PaginatedMedia<PartialMedia> {
        val imgCfg = settings.readImageConfig()
        val url = appendPage(catalog.url, page).replace("&&", "&")
        val response = FlxDispatchers.withIOContext {
            client.request(url = url)
                .execute()
                .fromJson<SearchPageDto<FilmSearchItemDto>>()
        }

        val results = response.results.fastMapNotNull { it.toPartialMedia(providerId, imgCfg = imgCfg) }
        return PaginatedMedia(
            page = response.page,
            results = results,
            hasNextPage = response.page < response.totalPages,
            totalPages = response.totalPages,
        )
    }

    private fun appendPage(url: String, page: Int): String =
        if (url.contains('?')) "$url&page=$page" else "$url?page=$page"
}
