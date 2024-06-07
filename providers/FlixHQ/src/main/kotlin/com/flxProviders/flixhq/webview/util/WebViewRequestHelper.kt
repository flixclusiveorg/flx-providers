package com.flxProviders.flixhq.webview.util

import com.flixclusive.core.util.log.errorLog
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.TvShow
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
                film = film,
                page = i
            )

            if (searchResponse.results.isEmpty())
                return null

            for (result in searchResponse.results) {
                if (result.tmdbId == film.id) {
                    id = result.id
                    break
                }

                val titleMatches = result.title.equals(film.title, ignoreCase = true)
                val filmTypeMatches = result.filmType?.type == film.filmType.type
                val releaseDateMatches =
                    result.releaseDate == film.dateReleased.split(" ").last()

                if (titleMatches && filmTypeMatches && film is TvShow) {
                    if (film.seasons.size == result.seasons || releaseDateMatches) {
                        id = result.id
                        break
                    }

                    val tvShowInfo = getFilmInfo(
                        filmId = result.id!!,
                        filmType = film.filmType
                    )

                    val tvReleaseDateMatches =
                        tvShowInfo.yearReleased == film.dateReleased.split("-").first()
                    val seasonCountMatches = film.seasons.size == tvShowInfo.seasons

                    if (tvReleaseDateMatches || seasonCountMatches) {
                        id = result.id
                        break
                    }
                }

                if (titleMatches && filmTypeMatches && releaseDateMatches) {
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