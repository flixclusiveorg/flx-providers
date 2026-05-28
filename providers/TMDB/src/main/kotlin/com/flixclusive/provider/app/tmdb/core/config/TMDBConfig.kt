package com.flixclusive.provider.app.tmdb.core.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.provider.extensions.getString
import java.util.Locale

internal const val TMDB_API_BASE_URL = "https://api.themoviedb.org/3/"
internal const val TMDB_IMAGE_BASE_ORIGINAL = "https://image.tmdb.org/t/p/original"
internal const val TMDB_IMAGE_BASE_W500 = "https://image.tmdb.org/t/p/w500"
internal const val TMDB_IMAGE_BASE_W200 = "https://image.tmdb.org/t/p/w200"
internal const val APPEND_TO_RESPONSE = "images,recommendations,external_ids,credits,keywords"
internal const val CACHE_MAX_AGE = 28800
internal const val CACHE_MAX_STALE = 604800
internal const val KEY_API_KEY = "tmdb_api_key"
internal const val KEY_CATALOGS = "tmdb_catalogs"
internal const val KEY_ADULT = "tmdb_include_adult"
internal const val KEY_LANGUAGE = "tmdb_language"
internal const val KEY_POSTER_PARTIAL = "tmdb_img_poster_partial"
internal const val KEY_POSTER_DETAIL = "tmdb_img_poster_detail"
internal const val KEY_BACKDROP_PARTIAL = "tmdb_img_backdrop_partial"
internal const val KEY_BACKDROP_DETAIL = "tmdb_img_backdrop_detail"
internal const val KEY_CACHE_MAX_AGE_PREF = "tmdb_cache_max_age"
internal const val KEY_CACHE_MAX_STALE_PREF = "tmdb_cache_max_stale"

internal val DEFAULT_LANGUAGE: String
    get() = Locale.getDefault().toLanguageTag()
        .takeIf { it != "und" && '-' in it } ?: "en-US"

internal data class ImageConfig(
    val posterPartialBase: String = TMDB_IMAGE_BASE_W500,
    val posterDetailBase: String = TMDB_IMAGE_BASE_W500,
    val backdropPartialBase: String = TMDB_IMAGE_BASE_W500,
    val backdropDetailBase: String = TMDB_IMAGE_BASE_ORIGINAL,
)

internal fun qualityToBase(quality: String?, default: String): String = when (quality) {
    "w200" -> TMDB_IMAGE_BASE_W200
    "w500" -> TMDB_IMAGE_BASE_W500
    "original" -> TMDB_IMAGE_BASE_ORIGINAL
    else -> default
}

internal suspend fun DataStore<Preferences>.readImageConfig(): ImageConfig = ImageConfig(
    posterPartialBase = qualityToBase(getString(KEY_POSTER_PARTIAL, null), TMDB_IMAGE_BASE_W500),
    posterDetailBase = qualityToBase(getString(KEY_POSTER_DETAIL, null), TMDB_IMAGE_BASE_W500),
    backdropPartialBase = qualityToBase(getString(KEY_BACKDROP_PARTIAL, null), TMDB_IMAGE_BASE_W500),
    backdropDetailBase = qualityToBase(getString(KEY_BACKDROP_DETAIL, null), TMDB_IMAGE_BASE_ORIGINAL),
)
