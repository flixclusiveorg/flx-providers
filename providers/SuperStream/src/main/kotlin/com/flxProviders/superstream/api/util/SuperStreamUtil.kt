package com.flxProviders.superstream.api.util

import com.flixclusive.core.util.film.FilmType
import java.text.SimpleDateFormat
import java.util.Locale

internal object SuperStreamUtil {
    // Random 32 length string
    fun randomToken(): String {
        return (0..31).joinToString("") {
            (('0'..'9') + ('a'..'f')).random().toString()
        }
    }

    enum class SSMediaType(val value: Int) {
        Series(2),
        Movies(1);

        fun toFilmType() = if (this == Series) FilmType.TV_SHOW else FilmType.MOVIE
        companion object {
            fun getSSMediaType(value: Int?): SSMediaType {
                return entries.firstOrNull { it.value == value } ?: Movies
            }
        }
    }

    fun getExpiryDate(): Long {
        // Current time + 12 hours
        return (System.currentTimeMillis() / 1000) + 60 * 60 * 12
    }

    fun String.raiseOnError(lazyMessage: String) {
        if(
            contains(
                other = "error",
                ignoreCase = true
            )
        ) throw Exception(lazyMessage)
    }

    fun String?.toValidReleaseDate(format: String = "MMMM d, yyyy"): String? {
        if(isNullOrBlank())
            return null

        val inputFormat = SimpleDateFormat(format, Locale.US)
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        return try {
            val date = inputFormat.parse(this)
            outputFormat.format(date)
        } catch (e: Exception) {
            throw Exception("Cannot parse release date of show.")
        }
    }
}