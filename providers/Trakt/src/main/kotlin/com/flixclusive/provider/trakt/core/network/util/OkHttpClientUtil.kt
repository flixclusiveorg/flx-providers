package com.flixclusive.provider.trakt.core.network.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.provider.trakt.core.network.interceptor.AuthKeyInterceptor
import com.flixclusive.provider.trakt.core.network.interceptor.TraktApiKeyInterceptor
import okhttp3.Cache
import okhttp3.Interceptor
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
        cacheSize: Long = 50L * 1024 * 1024, // default to 50 MB
        cacheMaxAge: Int = 60, // cache fresh for 1 minute
        cacheMaxStale: Int = 60 * 60 * 24 * 7 // accept stale cache up to 1 week when offline
    ): OkHttpClient {
        cache = if (::cache.isInitialized) cache else Cache(
            directory = File(context.cacheDir, CACHE_DIRECTORY_NAME),
            maxSize = cacheSize
        )

        val onlineInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            response.newBuilder()
                .header("Cache-Control", "public, max-age=$cacheMaxAge")
                .removeHeader("Pragma") // strip anti-cache headers from server
                .build()
        }

        val offlineInterceptor = Interceptor { chain ->
            var request = chain.request()
            if (!context.isNetworkAvailable()) {
                request = request.newBuilder()
                    .header("Cache-Control", "public, only-if-cached, max-stale=$cacheMaxStale")
                    .build()
            }
            chain.proceed(request)
        }

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

    @SuppressLint("MissingPermission")
    private fun Context.isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetwork?.let {
            cm.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } ?: false
    }
}