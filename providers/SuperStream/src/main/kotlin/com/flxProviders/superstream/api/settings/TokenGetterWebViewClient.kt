package com.flxProviders.superstream.api.settings

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.flixclusive.provider.settings.ProviderSettingsManager

private const val TOKEN_COOKIE_NAME = "ui"

class TokenGetterWebViewClient(
    private val settingsManager: ProviderSettingsManager,
    private val onTokenReceived: () -> Unit
) : WebViewClient() {
    private val cookieManager = CookieManager.getInstance()

    override fun onPageFinished(view: WebView?, url: String?) {
        if (url != null) {
            val token = getToken(url)

            if (token != null) {
                settingsManager.setInt(
                    key = TOKEN_STATUS_KEY,
                    `val` = TokenStatus.Online.ordinal
                )

                settingsManager.setString(
                    key = TOKEN_KEY,
                    `val` = token
                )

                onTokenReceived()
            }
        }
    }

    private fun getToken(url: String): String? {
        val cookies = cookieManager.getCookie(url)
        if (cookies == null || !cookies.contains("$TOKEN_COOKIE_NAME=")) {
            return null
        }

        return cookies.split("$TOKEN_COOKIE_NAME=")
            .getOrNull(1)
            ?.split(";")
            ?.getOrNull(0)
    }
}