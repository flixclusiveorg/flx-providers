package com.flixclusive.provider.app.trakt.core.network.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.provider.app.trakt.core.network.interceptor.AuthKeyInterceptor
import com.flixclusive.provider.app.trakt.core.network.interceptor.TraktApiKeyInterceptor
import com.flixclusive.provider.app.util.network.getOfflineInterceptor
import com.flixclusive.provider.app.util.network.getOnlineInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

internal object OkHttpClientUtil {
    private const val CACHE_DIRECTORY_NAME = "trakt_client_http_cache"

    private lateinit var cache: Cache

    fun deleteCache() {
        cache.delete()
    }

    fun createCachedClient(
        context: Context,
        settings: DataStore<Preferences>,
        cacheSize: Long = 25L * 1024 * 1024, // default to 50 MB
        cacheMaxAge: Int = 60, // cache fresh for 1 minute
        cacheMaxStale: Int = 60 * 60 * 24 * 7 // accept stale cache up to 1 week when offline
    ): OkHttpClient {
        cache = if (::cache.isInitialized) cache else Cache(
            directory = File(context.cacheDir, CACHE_DIRECTORY_NAME),
            maxSize = cacheSize
        ).also {
            cache = it
        }

        val onlineInterceptor = getOnlineInterceptor(cacheMaxAge = cacheMaxAge)
        val offlineInterceptor = getOfflineInterceptor(
            context = context,
            cacheMaxStale = cacheMaxStale
        )

        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(onlineInterceptor)
            .addInterceptor(offlineInterceptor)
            .addInterceptor(AuthKeyInterceptor(settings))
            .addInterceptor(TraktApiKeyInterceptor)
            .build()
    }

    fun createNonCachedClient(settings: DataStore<Preferences>): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthKeyInterceptor(settings))
            .addInterceptor(TraktApiKeyInterceptor)
            .build()
    }

    fun createVanillaClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }
}