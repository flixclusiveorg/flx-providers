package com.flixclusive.provider.app.stremio

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.flixclusive.provider.FlixclusiveProvider
import com.flixclusive.provider.ProviderPlugin
import com.flixclusive.provider.app.stremio.core.network.StremioClient
import com.flixclusive.provider.app.stremio.feature.catalog.StremioCatalogProvider
import com.flixclusive.provider.app.stremio.feature.crossmatch.StremioCrossMatcher
import com.flixclusive.provider.app.stremio.feature.link.StremioLinkProvider
import com.flixclusive.provider.app.stremio.feature.metadata.StremioMetadataProvider
import com.flixclusive.provider.app.stremio.feature.search.StremioSearchProvider
import com.flixclusive.provider.app.stremio.settings.StreamioScreen
import com.flixclusive.provider.capability.CatalogProviderApi
import com.flixclusive.provider.capability.CrossMatchProviderApi
import com.flixclusive.provider.capability.MediaLinkProviderApi
import com.flixclusive.provider.capability.MediaMetadataProviderApi
import com.flixclusive.provider.capability.SearchProviderApi
import okhttp3.OkHttpClient


@FlixclusiveProvider
class StremioPlugin : ProviderPlugin() {
    private lateinit var cachedClient: OkHttpClient
    private lateinit var linksClient: OkHttpClient

    private lateinit var searchProvider: StremioSearchProvider

    @Composable
    override fun SettingsScreen() {
        linksClient = when {
            ::linksClient.isInitialized -> linksClient
            else -> StremioClient.createCachedClient(
                context = LocalContext.current,
                cacheMaxAge = 60 * 3
            )
        }

        StreamioScreen(
            prefs = settings,
            client = linksClient
        )
    }

    override suspend fun getCatalogApi(context: Context): CatalogProviderApi {
        cachedClient = getCachedClient(context)

        return StremioCatalogProvider(
            client = cachedClient,
            plugin = this
        )
    }

    override suspend fun getCrossMatchApi(context: Context): CrossMatchProviderApi {
        cachedClient = getCachedClient(context)
        searchProvider = when {
            ::searchProvider.isInitialized -> searchProvider
            else -> StremioSearchProvider(
                client = cachedClient,
                plugin = this
            )
        }

        return StremioCrossMatcher(
            client = cachedClient,
            searchProvider = searchProvider,
            plugin = this
        )
    }

    override suspend fun getMediaLinkApi(context: Context): MediaLinkProviderApi {
        linksClient = when {
            ::linksClient.isInitialized -> linksClient
            else -> StremioClient.createCachedClient(
                context = context,
                cacheMaxAge = 60 * 3
            )
        }

        return StremioLinkProvider(
            client = linksClient,
            plugin = this
        )
    }

    override suspend fun getMetadataApi(context: Context): MediaMetadataProviderApi {
        cachedClient = getCachedClient(context)

        return StremioMetadataProvider(
            client = cachedClient,
            plugin = this
        )
    }

    override suspend fun getSearchApi(context: Context): SearchProviderApi {
        cachedClient = getCachedClient(context)
        searchProvider = when {
            ::searchProvider.isInitialized -> searchProvider
            else -> StremioSearchProvider(
                client = cachedClient,
                plugin = this
            )
        }

        return searchProvider
    }

    private fun getCachedClient(context: Context): OkHttpClient {
        return when {
            ::cachedClient.isInitialized -> cachedClient
            else -> StremioClient.createCachedClient(
                context = context,
                cacheMaxAge = 60 * 60 * 24 * 7
            )
        }
    }
}
