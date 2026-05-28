package com.flixclusive.provider.app.tmdb.core.network

import android.content.Context
import com.flixclusive.provider.app.util.network.getOfflineInterceptor
import com.flixclusive.provider.app.util.network.getOnlineInterceptor
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.File
import java.util.concurrent.TimeUnit

internal object TMDBClient {
    fun create(
        context: Context,
        apiKey: String,
        language: String,
        includeAdult: String,
        maxAge: Int,
        maxStale: Int,
        cacheSize: Long = 25L * 1024 * 1024,
    ): OkHttpClient {
        val cacheDir = File(context.cacheDir, "tmdb_http_cache")
        val cache = Cache(cacheDir, cacheSize)
        return OkHttpClient.Builder()
            .cache(cache)
            .addNetworkInterceptor(getOnlineInterceptor(maxAge))
            .addInterceptor(getOfflineInterceptor(context, maxStale))
            .addInterceptor(ApiKeyInterceptor(apiKey))
            .addInterceptor(GlobalPrefsInterceptor(language, includeAdult))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

private class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val newUrl = chain.request().url.newBuilder()
            .addQueryParameter("api_key", apiKey)
            .build()
        return chain.proceed(chain.request().newBuilder().url(newUrl).build())
    }
}

private class GlobalPrefsInterceptor(
    private val language: String,
    private val includeAdult: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host != "api.themoviedb.org") return chain.proceed(request)

        val newUrl = request.url.newBuilder()
            .setQueryParameter("language", language)
            .setQueryParameter("include_adult", includeAdult)
            .build()

        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}

