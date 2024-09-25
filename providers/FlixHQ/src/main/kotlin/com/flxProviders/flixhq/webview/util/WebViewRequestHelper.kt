package com.flxProviders.flixhq.webview.util

import com.flixclusive.core.util.log.errorLog
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.TvShow
import com.flxProviders.flixhq.api.FlixHQApi
import java.text.Normalizer

internal fun String.removeAccents(): String {
    return Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
}

internal fun String.toReferer(): String {
    return replaceFirst("^(movie|tv)/".toRegex(), "/watch-$1/")
}

internal suspend fun FlixHQApi.getMediaId(
    film: Film
): String? {
    return try {
        var i = 1
        var id: String? = null
        val maxPage = 3

        while (id == null) {
            if (i > maxPage) {
                return null
            }

            val searchResponse = search(
                title = film.title.removeAccents(),
                imdbId = film.imdbId,
                tmdbId = film.tmdbId,
                id = film.id,
                page = i
            )

            if (searchResponse.results.isEmpty())
                return null

            for (result in searchResponse.results) {
                val titleMatches = result.title.equals(film.title, ignoreCase = true)
                val filmTypeMatches = result.filmType.type == film.filmType.type
                val yearMatches =
                    result.year == film.year && film.year != null

                if (titleMatches && filmTypeMatches && film is TvShow) {
                    val tvShowInfo = getFilmDetails(film = film) as TvShow

                    val tvYearMatches =
                        tvShowInfo.year == film.year && film.year != null
                    val seasonCountMatches = film.seasons.size == tvShowInfo.totalSeasons

                    if (tvYearMatches || seasonCountMatches) {
                        id = result.id
                        break
                    }
                }

                if (titleMatches && filmTypeMatches && yearMatches) {
                    id = result.id
                    break
                }
            }

            if (searchResponse.hasNextPage) {
                i++
            } else if (id.isNullOrEmpty()) {
                break
            }
        }

        id
    } catch (e: Exception) {
        errorLog(e.stackTraceToString())
        null
    }
}