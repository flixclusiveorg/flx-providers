package com.flxProviders.stremio.api.model

import com.flixclusive.model.film.FilmDetails
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Genre
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.film.util.extractYear
import com.flxProviders.stremio.api.ADDON_SOURCE_KEY
import com.flxProviders.stremio.api.MEDIA_TYPE_KEY
import com.flxProviders.stremio.api.STREMIO
import com.google.gson.annotations.SerializedName

/**
 * This key/property is for Debrid caches that has the type of _other_
 * */
internal const val EMBEDDED_STREAM_KEY = "embedded_stream_key"
internal const val EMBEDDED_IMDB_ID_KEY = "embedded_imdb_id_key"

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
    internal val videos: List<MetaVideo>? = null,
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

    internal val imdbIdFromVideos: String?
        get() {
            val sampleId = videos?.find {
                it.id.startsWith("tt")
            }?.id

            return sampleId?.split(":")?.first()
        }

    private val isIPTV: Boolean
        get() = (type == "events" || type == "tv")
            && videos == null

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

            val unsortedEpisodes = videos?.mapNotNull {
                if ((it.season == null || it.episode == null) && type != "other")
                    return@mapNotNull null

                val episodeId = it.streams?.firstOrNull()
                    ?.url ?: it.id

                Episode(
                    id = episodeId,
                    title = it.title.replaceFirstChar { char ->
                        if (char == '/') Char.MIN_VALUE else char
                    } ?: "Episode ${it.episode}",
                    season = it.season ?: 1,
                    number = it.episode ?: episodeCount++,
                    image = it.thumbnail,
                    overview = it.overview ?: "",
                    airDate = released?.extractDate(),
                )
            }

            val comparator = compareBy<Episode> { it.season }
                .thenBy { it.number }

            return unsortedEpisodes?.sortedWith(comparator) ?: emptyList()
        }

    val filmType: FilmType
        get() {
            return when {
                type == "movie" || isIPTV -> FilmType.MOVIE
                type == "tv" || type == "series" -> FilmType.TV_SHOW
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
    val streams: List<StreamDto>? = null,
    @SerializedName("imdbSeason", alternate = ["season"]) val season: Int? = null,
    @SerializedName("imdbEpisode", alternate = ["episode"]) val episode: Int? = null
)

internal data class FetchMetaResponse(
    @SerializedName("meta") val film: Meta? = null,
    override val err: String?,
) : CommonErrorResponse()

internal fun Meta.toFilmDetails(addonName: String): FilmDetails {
    val properties = mutableMapOf(
        MEDIA_TYPE_KEY to type,
        ADDON_SOURCE_KEY to addonName
    )

    val embeddedImdbId = imdbIdFromVideos
    if (embeddedImdbId != null) {
        properties[EMBEDDED_IMDB_ID_KEY] = embeddedImdbId
    }

    return when (filmType) {
        FilmType.MOVIE -> {
            val debridCacheStreams = videos?.firstOrNull()?.streams
            val embeddedStreamUrl = if (
                type == "other"
                && debridCacheStreams?.isNotEmpty() == true
                && debridCacheStreams.first().url != null
            ) {
                debridCacheStreams.first().url
            } else null

            if (embeddedStreamUrl != null) {
                properties[EMBEDDED_STREAM_KEY] = embeddedStreamUrl
            }

            Movie(
                id = id,
                title = name,
                imdbId = imdbId,
                posterImage = poster,
                backdropImage = background,
                logoImage = logo,
                releaseDate = releaseInfo,
                overview = description,
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
                customProperties = properties.toMap()
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
                overview = description,
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
                customProperties = properties.toMap()
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
        overview = description,
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