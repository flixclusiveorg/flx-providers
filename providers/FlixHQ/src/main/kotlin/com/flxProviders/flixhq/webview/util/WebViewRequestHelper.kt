package com.flxProviders.flixhq.webview.util

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.TvShow
import com.flxProviders.flixhq.api.FlixHQApi
import java.net.URL
import javax.net.ssl.HttpsURLConnection

internal suspend fun FlixHQApi.getMediaId(
    film: Film
): String? {
    return try {
        var i = 1
        var id: String? = null
        val maxPage = 3

        while (id == null) {
            if (i > maxPage) {
                return ""
            }

            val searchResponse = search(
                query = film.title,
                page = i,
                filmType = film.filmType
            )

            if (searchResponse.results.isEmpty())
                return ""


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
                id = ""
                break
            }
        }

        id
    } catch (e: Exception) {
        errorLog(e.stackTraceToString())
        null
    }
}