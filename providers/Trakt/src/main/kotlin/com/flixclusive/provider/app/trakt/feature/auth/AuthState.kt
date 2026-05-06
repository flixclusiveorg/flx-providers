package com.flixclusive.provider.app.trakt.feature.auth

import com.flixclusive.provider.app.trakt.core.model.AuthToken

internal sealed class AuthState {
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class Expired(val data: AuthToken) : AuthState()
    data class Authenticated(val data: AuthToken) : AuthState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Authenticated) return false

            return data.accessToken == other.data.accessToken
        }

        override fun hashCode(): Int {
            return data.accessToken.hashCode()
        }
    }
}