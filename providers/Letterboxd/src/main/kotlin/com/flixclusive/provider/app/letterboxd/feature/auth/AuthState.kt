package com.flixclusive.provider.app.letterboxd.feature.auth

import com.flixclusive.provider.app.letterboxd.core.model.AuthToken

internal sealed interface AuthState {
    data object Loading : AuthState

    data object Unauthenticated : AuthState

    data class Expired(val token: AuthToken) : AuthState

    data class Authenticated(val token: AuthToken) : AuthState {
        // Keyed on the token alone so a refresh does not re-trigger effects.
        override fun equals(other: Any?) = other is Authenticated && other.token.accessToken == token.accessToken

        override fun hashCode() = token.accessToken.hashCode()
    }
}

internal fun AuthToken?.toAuthState(): AuthState =
    when {
        this == null -> AuthState.Unauthenticated
        isExpired && refreshToken.isBlank() -> AuthState.Expired(this)
        else -> AuthState.Authenticated(this)
    }
