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
    private val areVideosDebridTvShows: Boolean
        get() = videos?.firstOrNull()?.season != null
            && type == "other"

    private val areVideosCachedDebrid: Boolean
        get() = videos != null
            && videos.size > 1
            && type == "other"
            && !areVideosDebridTvShows

    val seasons: List<Season>
        get() {
            val traversedSeason = mutableMapOf<Int, String>()

            if (areVideosCachedDebrid) {
                return listOf(
                    Season(
                        number = 1,
                        name = "Cache",
                        episodes = episodes
                    )
                )
            }

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
                    name = "Season ${it.season}",
                    episodes = episodesForThisSeason
                )

                return@mapNotNull season
            } ?: emptyList()
        }

    @Suppress("USELESS_ELVIS")
    val episodes: List<Episode>
        get() {
            var episodeCount = 1
            return videos?.mapNotNull {
                if ((it.season == null || it.episode == null) && type != "other")
                    return@mapNotNull null

                Episode(
                    id = it.id,
                    title = it.title ?: "Episode ${it.episode}",
                    season = it.season ?: 1,
                    number = it.episode ?: episodeCount++,
                    image = it.thumbnail,
                    overview = it.overview ?: "",
                    airDate = released?.extractDate(),
                )
            } ?: emptyList()
        }

    val filmType: FilmType
        get() {
            return when {
                type == "tv" || type == "series" -> FilmType.TV_SHOW
                type == "movie" -> FilmType.MOVIE
                areVideosDebridTvShows || areVideosCachedDebrid -> FilmType.TV_SHOW
                else -> FilmType.MOVIE
            }
        }

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
    @SerializedName("title", alternate = ["name"]) val title: String,
    val releaseDate: String,
    val thumbnail: String? = null,
    val overview: String? = null,
    @SerializedName("imdbSeason", alternate = ["season"]) val season: Int? = null,
    @SerializedName("imdbEpisode", alternate = ["episode"]) val episode: Int? = null,
)

internal data class FetchMetaResponse(
    @SerializedName("meta") val film: Meta? = null,
    override val err: String?,
) : CommonErrorResponse()

internal fun Meta.toFilmDetails(addonName: String): FilmDetails {
    return when (filmType) {
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
        filmType = filmType,
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