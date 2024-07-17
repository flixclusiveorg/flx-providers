package com.flxProviders.superstream.api.settings

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import com.flixclusive.core.util.network.CryptographyUtil
import com.flixclusive.core.util.network.USER_AGENT
import com.flixclusive.provider.settings.ProviderSettingsManager

private const val GET_TOKEN_URL_ENCODED
    = "aHR0cHM6Ly93d3cuZmViYm94LmNvbS9sb2dpbi9nb29nbGU/anVtcD0lMkY="

@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
internal class TokenGetterWebView(
    context: Context,
    settingsManager: ProviderSettingsManager,
    onTokenReceived: () -> Unit
) : WebView(context) {
    val verticalScrollRange: Int
        get() = computeVerticalScrollRange() - height

    init {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString = USER_AGENT

        webViewClient = TokenGetterWebViewClient(
            settingsManager = settingsManager,
            onTokenReceived = onTokenReceived
        )

        loadUrl(CryptographyUtil.base64Decode(GET_TOKEN_URL_ENCODED))
    }
}