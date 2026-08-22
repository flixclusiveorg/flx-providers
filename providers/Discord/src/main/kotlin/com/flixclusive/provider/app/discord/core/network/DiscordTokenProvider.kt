package com.flixclusive.provider.app.discord.core.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.provider.app.discord.core.config.PrefsKey
import com.flixclusive.provider.app.discord.core.model.DiscordAuthToken
import com.flixclusive.provider.extensions.getObject
import com.flixclusive.provider.extensions.remove
import com.flixclusive.provider.extensions.setObject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DiscordTokenProvider(
    private val settings: DataStore<Preferences>,
    private val authService: DiscordAuthService,
) {
    private val mutex = Mutex()

    suspend fun current(): DiscordAuthToken? = settings.getObject<DiscordAuthToken>(PrefsKey.AUTH)

    suspend fun bearer(): String? =
        mutex.withLock {
            val token = current() ?: return null
            val fresh = if (token.needsRefresh) refreshLocked(token) else token
            fresh?.bearer
        }

    suspend fun forceRefresh(): String? =
        mutex.withLock {
            val token = current() ?: return null
            refreshLocked(token)?.bearer
        }

    suspend fun store(token: DiscordAuthToken) = settings.setObject(PrefsKey.AUTH, token)

    suspend fun clear() {
        settings.remove(PrefsKey.AUTH)
    }

    private suspend fun refreshLocked(token: DiscordAuthToken): DiscordAuthToken? {
        if (token.refreshToken.isBlank()) {
            clear()
            return null
        }

        return runCatching { authService.refresh(token.refreshToken) }
            .onSuccess { settings.setObject(PrefsKey.AUTH, it) }
            .onFailure {
                errorLog("Discord: token refresh failed — ${it.message}")
                clear()
            }.getOrNull()
    }
}
