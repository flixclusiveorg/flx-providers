package com.flixclusive.provider.app.letterboxd.core.config

import com.flixclusive.provider.app.letterboxd.BuildConfig
import java.net.URLEncoder

internal object LetterboxdConfig {
    const val BASE_URL = "https://api.letterboxd.com/api/v0"
    const val TOKEN_URL = "$BASE_URL/auth/token"

    // Letterboxd is migrating /film to /production. Keep the segment in one place.
    const val FILM_PATH = "film"

    const val MAX_PER_PAGE = 100

    val clientId: String get() = BuildConfig.LETTERBOXD_CLIENT_ID
    val clientSecret: String get() = BuildConfig.LETTERBOXD_CLIENT_SECRET

    val hasCredentials: Boolean get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
