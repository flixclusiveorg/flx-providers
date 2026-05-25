package com.flixclusive.provider.app.trakt.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.provider.app.trakt.core.config.PrefsKey
import com.flixclusive.provider.app.trakt.core.model.AuthToken
import com.flixclusive.provider.app.trakt.feature.auth.AuthState
import com.flixclusive.provider.app.trakt.feature.auth.user.SettingsToggleItem
import com.flixclusive.provider.extensions.getBoolAsFlow
import com.flixclusive.provider.extensions.getStringAsFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@OptIn(FlowPreview::class)
internal fun DataStore<Preferences>.observeAuthToken(): Flow<AuthState> =
    getStringAsFlow(PrefsKey.PREFS_AUTH)
        .map { raw ->
            if (raw == null) {
                AuthState.Unauthenticated
            } else {
                val response = runCatching { fromJson<AuthToken?>(raw) }.onFailure {
                    it.printStackTrace()
                    errorLog("Failed to parse auth token from preferences: ${it.message}")
                }.getOrNull()

                when {
                    response != null && !response.isExpired -> AuthState.Authenticated(response)
                    response != null && response.isExpired -> AuthState.Expired(response)
                    else -> AuthState.Unauthenticated
                }
            }
        }
        .onStart { emit(AuthState.Loading) }
        .debounce(800)
        .distinctUntilChanged()

@OptIn(ExperimentalCoroutinesApi::class)
internal fun DataStore<Preferences>.observeSettingToggles(): Flow<List<SettingsToggleItem>> {
    val keys = listOf(
        PrefsKey.PREFS_SCROBBLE,
        PrefsKey.PREFS_LIST_MANAGEMENT,
        PrefsKey.PREFS_CATALOGS
    )

    return getStringAsFlow(PrefsKey.PREFS_AUTH_USER_ID)
        .flatMapLatest { userId ->
            if (userId == null) return@flatMapLatest flowOf(emptyList())

            val userKeys = keys.map { PrefsKey.getPrefKeyForUser(userId, it) }
            combine(userKeys.map { key -> getBoolAsFlow(key, true) }) { values ->
                keys.mapIndexed { i, key ->
                    val userKey = userKeys[i]
                    SettingsToggleItem(
                        key = userKey,
                        title = when (key) {
                            PrefsKey.PREFS_SCROBBLE -> "Scrobble to Trakt"
                            PrefsKey.PREFS_LIST_MANAGEMENT -> "List Management"
                            PrefsKey.PREFS_CATALOGS -> "Display Catalogs"
                            else -> key
                        },
                        description = when (key) {
                            PrefsKey.PREFS_SCROBBLE -> "Automatically scrobble watched content to your Trakt profile."
                            PrefsKey.PREFS_LIST_MANAGEMENT -> "Enable advanced list management features like syncing and custom lists."
                            PrefsKey.PREFS_CATALOGS -> "Show Trakt catalogs in the app for easier discovery and tracking."
                            else -> null
                        },
                        value = values[i]
                    )
                }
            }
        }
        .distinctUntilChanged()
}
