package com.flixclusive.provider.app.discord.core.network.dto

import com.flixclusive.provider.app.discord.core.model.DiscordAuthToken
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_in") val expiresIn: Long = 0L,
    @SerialName("scope") val scope: String = "",
) {
    fun toAuthToken(): DiscordAuthToken =
        DiscordAuthToken(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = System.currentTimeMillis() + expiresIn * 1_000L,
            scope = scope,
        )
}
