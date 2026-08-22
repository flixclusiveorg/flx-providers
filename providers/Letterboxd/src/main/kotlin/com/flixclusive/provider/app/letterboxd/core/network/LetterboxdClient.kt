package com.flixclusive.provider.app.letterboxd.core.network

import android.content.Context
import com.flixclusive.provider.app.util.network.getOfflineInterceptor
import com.flixclusive.provider.app.util.network.getOnlineInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

internal object LetterboxdClient {
    private const val CACHE_DIRECTORY_NAME = "letterboxd_http_cache"
    private const val TIMEOUT_SECONDS = 20L

    /**
     * Reads are cached; writes and token exchanges go through [createVanillaClient] so a
     * cached response can never stand in for a mutation.
     */
    fun createCachedClient(
        context: Context,
        cacheSize: Long = 25L * 1024 * 1024,
        cacheMaxAge: Int = 60 * 5,
        cacheMaxStale: Int = 60 * 60 * 24 * 7,
    ): OkHttpClient =
        baseBuilder()
            .cache(Cache(File(context.cacheDir, CACHE_DIRECTORY_NAME), cacheSize))
            .addNetworkInterceptor(getOnlineInterceptor(cacheMaxAge = cacheMaxAge))
            .addInterceptor(getOfflineInterceptor(context = context, cacheMaxStale = cacheMaxStale))
            .build()

    fun createVanillaClient(): OkHttpClient = baseBuilder().build()

    private fun baseBuilder() =
        OkHttpClient
            .Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
}
