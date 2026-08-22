package com.flixclusive.provider.app.letterboxd.core.config

internal object PrefsKey {
    const val MEMBER_AUTH = "letterboxd_prefs_member_auth"
    const val APP_AUTH = "letterboxd_prefs_app_auth"
    const val MEMBER = "letterboxd_prefs_member"

    const val LIST_MANAGEMENT = "letterboxd_prefs_list_management"
    const val DIARY_LOGGING = "letterboxd_prefs_diary_logging"
    const val MARK_WATCHED = "letterboxd_prefs_mark_watched"

    const val DEFAULT_LIST_MANAGEMENT = true

    // Both write to a public profile, so they stay off until the user opts in.
    const val DEFAULT_DIARY_LOGGING = false
    const val DEFAULT_MARK_WATCHED = false
}
