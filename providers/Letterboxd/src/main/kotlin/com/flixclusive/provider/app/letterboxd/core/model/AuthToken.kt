package com.flixclusive.provider.app.letterboxd.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val REFRESH_WINDOW_MS = 5 * 60 * 1000L

@Serializable
internal data class AuthToken(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_at") val expiresAt: Long = 0L,
    @SerialName("token_type") val tokenType: String = "Bearer",
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() >= expiresAt

    val needsRefresh: Boolean
        get() = System.currentTimeMillis() >= expiresAt - REFRESH_WINDOW_MS

    val bearer: String
        get() = "Bearer $accessToken"
}
