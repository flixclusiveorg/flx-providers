package com.flxProviders.superstream.settings

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.flixclusive.provider.settings.ProviderSettings

private const val TOKEN_COOKIE_NAME = "ui"

class TokenGetterWebViewClient(
    private val settings: ProviderSettings,
    private val onTokenReceived: () -> Unit
) : WebViewClient() {
    private val cookieManager = CookieManager.getInstance()

    override fun onPageFinished(view: WebView?, url: String?) {
        if (url != null) {
            val token = getToken(url)

            if (token != null) {
                settings.setInt(
                    key = TOKEN_STATUS_KEY,
                    `val` = TokenStatus.Online.ordinal
                )

                settings.setString(
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