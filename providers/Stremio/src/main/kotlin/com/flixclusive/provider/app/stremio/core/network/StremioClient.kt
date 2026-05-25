package com.flixclusive.provider.app.stremio.core.network

import android.content.Context
import com.flixclusive.provider.app.util.network.getOfflineInterceptor
import com.flixclusive.provider.app.util.network.getOnlineInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

internal object StremioClient {
    private const val CACHE_DIRECTORY_NAME = "stremio_client_http_cache"

    private lateinit var cache: Cache

    fun deleteCache() {
        cache.delete()
    }

    fun createCachedClient(
        context: Context,
        cacheMaxAge: Int = 60, // cache fresh for 1 minute
        cacheMaxStale: Int = 60 * 60 * 24 * 7, // accept stale cache up to 1 week when offline
        cacheSize: Long = 25L * 1024 * 1024 // default to 50 MB
    ): OkHttpClient {
        cache = when {
            ::cache.isInitialized -> cache
            else -> Cache(
                directory = File(context.cacheDir, CACHE_DIRECTORY_NAME),
                maxSize = cacheSize
            ).also {
                cache = it
            }
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
            .build()
    }
}