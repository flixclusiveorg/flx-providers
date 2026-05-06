package com.flxProviders.stremio.api.model

import androidx.compose.ui.util.fastMap
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.Genre
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.media.common.tv.Season
import com.flxProviders.stremio.api.ADDON_SOURCE_KEY
import com.flxProviders.stremio.api.MEDIA_TYPE_KEY
import com.flxProviders.stremio.api.util.toMs
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlin.time.Clock

/**
 * This key/property is for Debrid caches that has the type of _other_
 * */
internal const val EMBEDDED_STREAM_KEY = "embedded_stream_key"
internal const val EMBEDDED_IMDB_ID_KEY = "embedded_imdb_id_key"

@Serializable
internal data class Meta(
    val id: String,
    val type: String,
    val name: String,
    val description: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val released: String? = null,
    val language: String? = null,
    val runtime: String? = null,
    val website: String? = null,
    val genres: List<String>? = null,
    internal val videos: List<MetaVideo>? = null,
    @SerialName("releaseInfo") val year: String? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("imdbRating") val rating: String? = null,
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
                        id = videos?.firstOrNull()?.id ?: "cache_season",
                        number = 1,
                        title = "Cache",
                        episodes = episodes,
                        isReleased = true
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
                    id = it.id,
                    number = it.season,
                    title = "Season ${it.season}",
                    episodes = episodesForThisSeason,
                    isReleased = true,
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

                val releaseDate = released?.toMs()
                val isReleased = releaseDate != null && releaseDate <= Clock.System.now().toEpochMilliseconds()

                Episode(
                    id = episodeId,
                    title = it.title.replaceFirstChar { char ->
                        if (char == '/') Char.MIN_VALUE else char
                    } ?: "Episode ${it.episode}",
                    season = it.season ?: 1,
                    number = it.episode ?: episodeCount++,
                    image = it.thumbnail,
                    overview = it.overview ?: "",
                    releaseDate = released?.toMs(),
                    isReleased = isReleased,
                )
            }

            val comparator = compareBy<Episode> { it.season }
                .thenBy { it.number }

            return unsortedEpisodes?.sortedWith(comparator) ?: emptyList()
        }

    val mediaType: MediaType
        get() {
            return when {
                type == "movie" || isIPTV -> MediaType.MOVIE
                type == "tv" || type == "series" -> MediaType.SHOW
                areVideosDebridTvShows || areVideosCachedDebrid -> MediaType.SHOW
                else -> MediaType.MOVIE
            }
        }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class MetaVideo(
    val id: String,
    @JsonNames("title", "name") val title: String,
    val releaseDate: String,
    val thumbnail: String? = null,
    val overview: String? = null,
    val streams: List<StreamDto>? = null,
    @JsonNames("imdbSeason", "season") val season: Int? = null,
    @JsonNames("imdbEpisode", "episode") val episode: Int? = null
)

internal data class FetchMetaResponse(
    @SerialName("meta") val film: Meta? = null,
    override val err: String?,
) : CommonErrorResponse()

internal fun Meta.toMedia(
    addonName: String,
    providerId: String,
): MediaMetadata {
    val properties = mutableMapOf(
        MEDIA_TYPE_KEY to type,
        ADDON_SOURCE_KEY to addonName
    )

    val embeddedImdbId = imdbIdFromVideos
    if (embeddedImdbId != null) {
        properties[EMBEDDED_IMDB_ID_KEY] = embeddedImdbId
    }

    val externalIds = buildMap {
        if (imdbId != null) {
            put(MediaIdSource.IMDB, imdbId)
        }
    }

    return when (mediaType) {
        MediaType.MOVIE -> {
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
                externalIds = externalIds,
                posterImage = poster,
                backdropImage = background,
                logoImage = logo,
                releaseDate = released?.toMs(),
                overview = description,
                language = language,
                rating = rating?.toDoubleOrNull(),
                homePage = website,
                customProperties = properties.toMap(),
                providerId = providerId,
                genres = genres?.map {
                    Genre(name = it)
                } ?: emptyList()
            )
        }
        MediaType.SHOW -> {
            Show(
                id = id,
                externalIds = externalIds,
                title = name,
                posterImage = poster,
                backdropImage = background,
                logoImage = logo,
                releaseDate = released?.toMs(),
                overview = description,
                language = language,
                rating = rating?.toDoubleOrNull(),
                providerId = providerId,
                homePage = website,
                genres = genres?.map {
                    Genre(name = it)
                } ?: emptyList(),
                seasons = seasons,
                totalSeasons = seasons.size,
                totalEpisodes = episodes.size,
                customProperties = properties.toMap()
            )
        }
    }
}

internal fun Meta.toPartialMedia(
    addonName: String,
    providerId: String
): PartialMedia {
    val externalIds = buildMap {
        if (imdbId != null) {
            put(MediaIdSource.IMDB, imdbId)
        }
    }

    return PartialMedia(
        id = id,
        externalIds = externalIds,
        title = name,
        posterImage = poster,
        backdropImage = background,
        logoImage = logo,
        releaseDate = released?.toMs(),
        language = language,
        overview = description,
        rating = rating?.toDoubleOrNull(),
        type = mediaType,
        homePage = website,
        genres = genres?.fastMap { Genre(name = it) } ?: emptyList(),
        providerId = providerId,
        customProperties = mapOf(
            MEDIA_TYPE_KEY to type,
            ADDON_SOURCE_KEY to addonName,
        )
    )
}