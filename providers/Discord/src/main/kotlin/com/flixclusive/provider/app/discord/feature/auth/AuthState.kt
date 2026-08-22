package com.flixclusive.provider.app.discord.feature.auth

import com.flixclusive.provider.app.discord.core.model.DiscordAuthToken

internal sealed interface AuthState {
    data object Loading : AuthState

    data object Unauthenticated : AuthState

    data class Expired(val token: DiscordAuthToken) : AuthState

    data class Authenticated(val token: DiscordAuthToken) : AuthState {
        override fun equals(other: Any?) = other is Authenticated && other.token.accessToken == token.accessToken

        override fun hashCode() = token.accessToken.hashCode()
    }
}

internal fun DiscordAuthToken?.toAuthState(): AuthState =
    when {
        this == null -> AuthState.Unauthenticated
        isExpired -> AuthState.Expired(this)
        else -> AuthState.Authenticated(this)
    }
