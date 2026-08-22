package com.flixclusive.provider.app.letterboxd.core.network

import kotlinx.serialization.json.Json

/**
 * Request bodies encode their defaults — `AppJson` omits them — and drop nulls so an
 * absent optional never reaches Letterboxd as an explicit `null`.
 */
internal val LetterboxdJson =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }
