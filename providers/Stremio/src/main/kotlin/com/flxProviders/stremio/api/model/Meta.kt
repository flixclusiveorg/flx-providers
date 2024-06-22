package com.flxProviders.stremio.api.model

import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.film.extractYear
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.Genre
import com.flixclusive.model.tmdb.Movie
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.model.tmdb.common.tv.Season
import com.flxProviders.stremio.api.ADDON_SOURCE_KEY
import com.flxProviders.stremio.api.MEDIA_TYPE_KEY
import com.flxProviders.stremio.api.STREMIO
import com.google.gson.annotations.SerializedName
import java.time.format.DateTimeFormatter

internal data class Meta(
    val id: String,
    val type: String,
    val name: String,
    val description: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val releaseInfo: String? = null,
    val released: String? = null,
    val language: String? = null,
    val runtime: String? = null,
    val website: String? = null,
    val genres: List<String>? = null,
    private val videos: List<MetaVideo>? = null,
    @SerializedName("imdb_id") val imdbId: String? = null,
    @SerializedName("imdbRating") val rating: String? = null,
) {
    val seasons: List<Season>
        get() {
            val traversedSeason = mutableMapOf<Int, String>()
            return videos?.mapNotNull {
                if (traversedSeason.contains(it.season) || it.season == null) {
                    return@mapNotNull null
                }

                traversedSeason[it.season] = it.id

                val episodesForThisSeason = episodes.filter { episode ->
                    episode.season == it.season
                }

                val season = Season(
                    number = it.season,
                    episodes = episodesForThisSeason
                )

                return@mapNotNull season
            } ?: emptyList()
        }

    val episodes: List<Episode>
        get() = videos?.mapNotNull {
            if (it.season == null || it.episode == null)
                return@mapNotNull null

            Episode(
                id = it.id,
                title = it.title,
                season = it.season,
                number = it.episode,
                image = it.thumbnail,
                overview = it.overview ?: "",
                airDate = released?.extractDate(),
            )
        } ?: emptyList()

    private fun String.extractDate(): String {
        val parts = split("T")
        if (parts.size != 2) {
            return this
        }

        val datePart = parts[0]
        return datePart
    }
}

internal data class MetaVideo(
    val id: String,
    val title: String,
    val releaseDate: String,
    val thumbnail: String? = null,
    val overview: String? = null,
    @SerializedName("imdbSeason", alternate = ["season"]) val season: Int? = null,
    @SerializedName("imdbEpisode", alternate = ["episode"]) val episode: Int? = null
)

internal data class FetchMetaResponse(
    @SerializedName("meta") val film: Meta? = null,
    override val err: String?,
) : CommonErrorResponse()

internal fun Meta.toFilmDetails(addonName: String): FilmDetails {
    return when (type.getType()) {
        FilmType.MOVIE -> {
            Movie(
                id = id,
                title = name,
                imdbId = imdbId,
                posterImage = poster,
                backdropImage = background,
                logoImage = logo,
                releaseDate = releaseInfo,
                language = language,
                rating = rating?.toDoubleOrNull(),
                year = releaseInfo?.extractYear(),
                providerName = STREMIO,
                homePage = website,
                genres = genres?.map {
                    Genre(
                        id = -1,
                        name = it
                    )
                } ?: emptyList(),
                customProperties = mapOf(
                    MEDIA_TYPE_KEY to type,
                    ADDON_SOURCE_KEY to addonName,
                )
            )
        }
        FilmType.TV_SHOW -> {
            TvShow(
                id = id,
                title = name,
                imdbId = imdbId,
                posterImage = poster,
                backdropImage = background,
                logoImage = logo,
                releaseDate = releaseInfo,
                parsedReleaseDate = releaseInfo,
                language = language,
                rating = rating?.toDoubleOrNull(),
                year = releaseInfo?.extractYear(),
                providerName = STREMIO,
                homePage = website,
                genres = genres?.map {
                    Genre(
                        id = -1,
                        name = it
                    )
                } ?: emptyList(),
                seasons = seasons,
                totalSeasons = seasons.size,
                totalEpisodes = episodes.size,
                customProperties = mapOf(
                    MEDIA_TYPE_KEY to type,
                    ADDON_SOURCE_KEY to addonName,
                )
            )
        }
    }
}

internal fun Meta.toFilmSearchItem(addonName: String): FilmSearchItem {
    return FilmSearchItem(
        id = id,
        title = name,
        posterImage = poster,
        backdropImage = background,
        logoImage = logo,
        releaseDate = releaseInfo,
        language = language,
        rating = rating?.toDoubleOrNull(),
        year = releaseInfo?.extractYear(),
        providerName = STREMIO,
        filmType = type.getType(),
        homePage = website,
        genres = genres?.map {
            Genre(
                id = -1,
                name = it
            )
        } ?: emptyList(),
        customProperties = mapOf(
            MEDIA_TYPE_KEY to type,
            ADDON_SOURCE_KEY to addonName,
        )
    )
}

private fun String.getType(): FilmType {
    return when (this) {
        "tv", "series" -> FilmType.TV_SHOW
        "movie" -> FilmType.MOVIE
        else -> FilmType.MOVIE
    }
}