package com.flixclusive.provider.trakt.feature.auth.user

import com.flixclusive.provider.trakt.core.model.TraktUser

internal sealed class UserState {
    data object Loading : UserState()
    data object LoggingOut : UserState()
    data class Success(val user: TraktUser) : UserState()
    data class Error(val message: String) : UserState()
}