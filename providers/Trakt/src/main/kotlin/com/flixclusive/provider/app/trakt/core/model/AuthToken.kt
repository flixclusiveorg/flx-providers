package com.flixclusive.provider.app.trakt.core.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AuthToken(
    @SerialName("access_token") val accessToken: String,
    @SerialName("scope") val scope: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") private val expiresIn: Int,
    @SerialName("created_at") private val createdAt: Int
) {
    val isExpired: Boolean get() {
        val currentTime = System.currentTimeMillis() / 1000
        return currentTime >= createdAt + expiresIn
    }
}