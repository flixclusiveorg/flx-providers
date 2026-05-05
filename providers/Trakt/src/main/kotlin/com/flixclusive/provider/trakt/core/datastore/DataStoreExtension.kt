package com.flixclusive.provider.trakt.core.datastore

import androidx.compose.ui.util.fastMapNotNull
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.provider.trakt.core.config.PrefsKey
import com.flixclusive.provider.trakt.core.model.AuthToken
import com.flixclusive.provider.trakt.feature.auth.AuthState
import com.flixclusive.provider.trakt.feature.auth.user.SettingsToggleItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transform

internal fun DataStore<Preferences>.observeAuthToken(): Flow<AuthState> {
    return data.transform { prefs ->
        emit(AuthState.Loading)
        val `object` = prefs[stringPreferencesKey(PrefsKey.PREFS_AUTH)]

        if (`object` == null) {
            emit(AuthState.Unauthenticated)
            return@transform
        }

        val response = runCatching {
            fromJson<AuthToken>(`object`)
        }.onFailure {
            it.printStackTrace()
            errorLog("Failed to parse auth token from preferences: ${it.message}")
        }.getOrNull()

        if (response != null && !response.isExpired) {
            emit(AuthState.Authenticated(response))
        } else if (response != null && response.isExpired) {
            emit(AuthState.Expired(response))
        } else {
            emit(AuthState.Unauthenticated)
        }
    }.distinctUntilChanged()
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun DataStore<Preferences>.observeSettingToggles(): Flow<List<SettingsToggleItem>> {
    return data.mapLatest { prefs ->
        val keys = listOf(
            PrefsKey.PREFS_SCROBBLE,
            PrefsKey.PREFS_LIST_MANAGEMENT,
            PrefsKey.PREFS_CATALOGS
        )

        keys.fastMapNotNull { key ->
            val userId = prefs[stringPreferencesKey(PrefsKey.PREFS_AUTH_USER_ID)] ?: return@fastMapNotNull null

            val userSpecificKey = PrefsKey.getPrefKeyForUser(userId, key)
            val value = prefs[booleanPreferencesKey(userSpecificKey)] ?: true
            SettingsToggleItem(
                key = userSpecificKey,
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
                value = value
            )
        }
    }.distinctUntilChanged()
}