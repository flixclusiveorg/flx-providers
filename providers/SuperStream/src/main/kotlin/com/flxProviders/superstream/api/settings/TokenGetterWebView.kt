package com.flxProviders.superstream.api.settings

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import com.flixclusive.core.util.network.CryptographyUtil
import com.flixclusive.core.util.network.USER_AGENT
import com.flixclusive.core.util.network.getRandomUserAgent
import com.flixclusive.provider.settings.ProviderSettings

private const val GET_TOKEN_URL_ENCODED
    = "aHR0cHM6Ly93d3cuZmViYm94LmNvbS9sb2dpbi9nb29nbGU/anVtcD0lMkY="

@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
internal class TokenGetterWebView(
    context: Context,
    settings: ProviderSettings,
    onTokenReceived: () -> Unit
) : WebView(context) {
    val verticalScrollRange: Int
        get() = computeVerticalScrollRange() - height

    init {
        this.settings.javaScriptEnabled = true
        this.settings.domStorageEnabled = true
        this.settings.userAgentString = getRandomUserAgent()

        webViewClient = TokenGetterWebViewClient(
            settings = settings,
            onTokenReceived = onTokenReceived
        )

        loadUrl(CryptographyUtil.base64Decode(GET_TOKEN_URL_ENCODED))
    }
}