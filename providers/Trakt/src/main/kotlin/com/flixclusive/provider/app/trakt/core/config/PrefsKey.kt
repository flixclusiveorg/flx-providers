package com.flixclusive.provider.app.trakt.core.config

object PrefsKey {
    const val PREFS_AUTH = "trakt_prefs_auth"
    const val PREFS_AUTH_USER_ID = "trakt_prefs_auth_user_id"
    const val PREFS_SCROBBLE = "trakt_prefs_scrobble"
    const val PREFS_LIST_MANAGEMENT = "trakt_prefs_list_management"
    const val PREFS_CATALOGS = "trakt_prefs_catalogs"

    fun getPrefKeyForUser(userId: String, baseKey: String): String {
        return "$baseKey$userId"
    }
}