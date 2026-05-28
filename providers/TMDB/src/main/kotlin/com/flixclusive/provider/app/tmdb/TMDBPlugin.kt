package com.flixclusive.provider.app.tmdb

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.flixclusive.provider.FlixclusiveProvider
import com.flixclusive.provider.ProviderPlugin
import com.flixclusive.provider.app.tmdb.core.config.CACHE_MAX_AGE
import com.flixclusive.provider.app.tmdb.core.config.CACHE_MAX_STALE
import com.flixclusive.provider.app.tmdb.core.config.DEFAULT_LANGUAGE
import com.flixclusive.provider.app.tmdb.core.config.KEY_ADULT
import com.flixclusive.provider.app.tmdb.core.config.KEY_API_KEY
import com.flixclusive.provider.app.tmdb.core.config.KEY_CACHE_MAX_AGE_PREF
import com.flixclusive.provider.app.tmdb.core.config.KEY_CACHE_MAX_STALE_PREF
import com.flixclusive.provider.app.tmdb.core.config.KEY_LANGUAGE
import com.flixclusive.provider.app.tmdb.core.network.TMDBClient
import com.flixclusive.provider.app.tmdb.feature.catalog.TMDBCatalogProvider
import com.flixclusive.provider.app.tmdb.feature.crossmatch.TMDBCrossMatcher
import com.flixclusive.provider.app.tmdb.feature.link.TMDBLinkProvider
import com.flixclusive.provider.app.tmdb.feature.metadata.TMDBMetadataProvider
import com.flixclusive.provider.app.tmdb.feature.search.TMDBSearchProvider
import com.flixclusive.provider.app.tmdb.settings.TMDBSettingsScreen
import com.flixclusive.provider.capability.CatalogProviderApi
import com.flixclusive.provider.capability.CrossMatchProviderApi
import com.flixclusive.provider.capability.MediaLinkProviderApi
import com.flixclusive.provider.capability.MediaMetadataProviderApi
import com.flixclusive.provider.capability.SearchProviderApi
import com.flixclusive.provider.extensions.getString
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import java.io.File

@FlixclusiveProvider
class TMDBPlugin : ProviderPlugin() {
    private val clientMutex = Mutex()
    private var _client: OkHttpClient? = null

    private suspend fun getClient(context: Context): OkHttpClient {
        return _client ?: clientMutex.withLock {
            _client ?: TMDBClient.create(
            context = context,
            apiKey = settings.getString(KEY_API_KEY, null) ?: BuildConfig.TMDB_API_KEY,
            language = settings.getString(KEY_LANGUAGE, null) ?: DEFAULT_LANGUAGE,
            includeAdult = settings.getString(KEY_ADULT, null) ?: "false",
            maxAge = settings.getString(KEY_CACHE_MAX_AGE_PREF, null)?.toIntOrNull() ?: CACHE_MAX_AGE,
            maxStale = settings.getString(KEY_CACHE_MAX_STALE_PREF, null)?.toIntOrNull() ?: CACHE_MAX_STALE,
            ).also { _client = it }
        }
    }

    override suspend fun getCatalogApi(context: Context): CatalogProviderApi {
        return TMDBCatalogProvider(
            client = getClient(context),
            settings = settings,
            context = context,
            providerId = id,
        )
    }

    override suspend fun getSearchApi(context: Context): SearchProviderApi {
        return TMDBSearchProvider(
            client = getClient(context),
            settings = settings,
            providerId = id,
        )
    }

    override suspend fun getMetadataApi(context: Context): MediaMetadataProviderApi {
        return TMDBMetadataProvider(
            client = getClient(context),
            settings = settings,
            providerId = id,
        )
    }

    override suspend fun getMediaLinkApi(context: Context): MediaLinkProviderApi {
        return TMDBLinkProvider(
            client = getClient(context),
            settings = settings,
        )
    }

    override suspend fun getCrossMatchApi(context: Context): CrossMatchProviderApi {
        return TMDBCrossMatcher(
            client = getClient(context),
            settings = settings,
            providerId = id,
        )
    }

    @Composable
    override fun SettingsScreen() {
        val context = LocalContext.current
        TMDBSettingsScreen(
            settings = settings,
            onClearCache = { _client?.cache?.evictAll() },
            cacheDir = File(context.cacheDir, "tmdb_http_cache"),
        )
    }
}
