package com.flixclusive.provider.trakt.core.network.interceptor

import com.flixclusive.provider.app.trakt.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

internal object TraktApiKeyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newRequest = request.newBuilder()
            .addHeader("trakt-api-version", "2")
            .addHeader("trakt-api-key", BuildConfig.TRAKT_CLIENT_ID )
            .build()
        
        return chain.proceed(newRequest)
    }
}