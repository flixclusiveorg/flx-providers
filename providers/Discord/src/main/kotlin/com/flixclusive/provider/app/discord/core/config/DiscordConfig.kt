package com.flixclusive.provider.app.discord.core.config

import java.net.URLEncoder

internal object DiscordConfig {
    const val CLIENT_ID = "1540624913931567196"

    const val OAUTH_AUTHORIZE_URL = "https://discord.com/oauth2/authorize"
    const val OAUTH_TOKEN_URL = "https://discord.com/api/v10/oauth2/token"
    const val SCOPES = "openid sdk.social_layer_presence"

    const val GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json"

    const val APP_NAME = "Flixclusive"
    const val DOWNLOAD_URL = "https://github.com/flixclusiveorg/Flixclusive/releases"
    const val DOWNLOAD_BUTTON_LABEL = "Get Flixclusive"

    fun externalAssetsUrl() = "https://discord.com/api/v9/applications/$CLIENT_ID/external-assets"

    fun redirectUri(providerId: String) = "flixclusive://provider/$providerId/settings"

    fun authorizeUrl(
        providerId: String,
        codeChallenge: String,
        state: String,
    ): String = buildString {
        append(OAUTH_AUTHORIZE_URL)
        append("?client_id=").append(CLIENT_ID)
        append("&response_type=code")
        append("&redirect_uri=").append(encode(redirectUri(providerId)))
        append("&scope=").append(encode(SCOPES))
        append("&state=").append(state)
        append("&code_challenge_method=S256")
        append("&code_challenge=").append(codeChallenge)
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
}
