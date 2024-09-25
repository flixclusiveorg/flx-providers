package com.flxProviders.flixhq.api.util

import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.film.FilmSearchItem
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.Locale

internal fun Elements.getEpisodeId(episode: Int): String? {
    return try {
        var episodeId: String? = null

        val episodeDoc = select("a.eps-item:contains(Eps $episode)")

        if (episodeDoc.isNotEmpty()) {
            episodeId = episodeDoc.attr("data-id")
        }

        return episodeId
    } catch (e: Exception) {
        null
    }
}

internal fun Elements.getSeasonId(season: Int): String? {
    return try {
        var seasonId: String? = null

        if (size == 1 && season == 1 || this[season - 1].text().contains("Season $season")) {
            return this[season - 1].attr("data-id")
        }

        val seasonDoc = select("a:contains(Season ${season})")

        if (seasonDoc.isNotEmpty()) {
            seasonId = seasonDoc.attr("data-id")
        }

        return seasonId
    } catch (e: Exception) {
        null
    }
}

internal fun String.replaceWhitespaces(toReplace: String) = replace(
    Regex("[\\s_]+"),
    toReplace
)

internal fun Element.toFilmSearchItem(
    baseUrl: String,
    provider: String
): FilmSearchItem {
    val filmType = when {
        select("div.film-detail > div.fd-infor > span.float-right").text() == "Movie" -> FilmType.MOVIE
        else -> FilmType.TV_SHOW
    }

    val year = if (filmType == FilmType.MOVIE) {
        val rawYear = select("div.film-detail > div.fd-infor > span:nth-child(1)").text()

        rawYear.toIntOrNull()
    } else null


    return FilmSearchItem(
        id = select("div.film-poster > a").attr("href").substring(1),
        providerName = provider,
        title = select("div.film-detail > h2 > a").attr("title"),
        posterImage = select("div.film-poster > img").attr("data-src"),
        homePage = "${baseUrl}${select("div.film-poster > a").attr("href")}",
        year = year,
        filmType = filmType
    )
}

internal fun Elements.getServerName(mediaId: String): String {
    val anchorElement = select("a")
    val titleElement = anchorElement.attr("title")

    return if (mediaId.contains("movie")) {
        titleElement
            .lowercase(Locale.US)
    } else {
        titleElement.substring(6)
            .trim()
            .lowercase(Locale.US)
    }
}

internal fun Elements.getServerUrl(
    baseUrl: String,
    mediaId: String
): String {
    return "${baseUrl}/${mediaId}.${
        if (!mediaId.contains("movie")) {
            attr("data-id")
        } else {
            attr("data-linkid")
        }
    }".replace(
        if (!mediaId.contains("movie")) {
            Regex("/tv/")
        } else {
            Regex("/movie/")
        },
        if (!mediaId.contains("movie")) {
            "/watch-tv/"
        } else {
            "/watch-movie/"
        }
    )
}