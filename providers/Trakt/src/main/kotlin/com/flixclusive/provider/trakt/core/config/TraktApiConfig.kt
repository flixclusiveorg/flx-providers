package com.flixclusive.provider.trakt.core.config

import com.flixclusive.provider.app.trakt.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

internal object TraktApiConfig {
    const val API_BASE_URL = "https://apiz.trakt.tv"
    const val AUTH_BASE_URL = "https://trakt.tv"

    const val AUTH_TOKEN_URL = "$API_BASE_URL/oauth/token"

    const val SEARCH_V2_URL = "https://search.trakt.tv/multi_search"

    const val WATCHNOW_SOURCE_LOCATION_GETTER_URL = "https://ipinfo.io/country"

    const val PAGE_RESULTS_LIMIT = 20

    private fun getRedirectUri(providerId: String): String {
        val redirectUri = "flixclusive://provider/$providerId/settings"
        return URLEncoder.encode(redirectUri, "UTF-8")
    }

    fun getAuthorizeAppUri(providerId: String): String {
        val redirectUri = getRedirectUri(providerId)
        return "$AUTH_BASE_URL/oauth/authorize" +
                "?client_id=${BuildConfig.TRAKT_CLIENT_ID}" +
                "&redirect_uri=$redirectUri" +
                "&response_type=code" +
                "&scope=public+openid+profile+email"
    }

    fun getExchangeTokenRequest(
        providerId: String,
        code: String,
        isRefreshing: Boolean = false
    ): Request {
        val redirectUri = getRedirectUri(providerId)
        val grantType = if (isRefreshing) "refresh_token" else "authorization_code"
        val codeParam = if (isRefreshing) "refresh_token" else "code"

        val body = "$codeParam=$code" +
                "&client_id=${BuildConfig.TRAKT_CLIENT_ID}" +
                "&client_secret=${BuildConfig.TRAKT_CLIENT_SECRET}" +
                "&redirect_uri=$redirectUri" +
                "&grant_type=$grantType"

        return Request.Builder()
            .url(AUTH_TOKEN_URL)
            .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()
    }
}