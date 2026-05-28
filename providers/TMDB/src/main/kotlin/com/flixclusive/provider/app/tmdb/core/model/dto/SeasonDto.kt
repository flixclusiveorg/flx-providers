package com.flixclusive.provider.app.tmdb.core.model.dto

import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.provider.app.tmdb.core.config.TMDB_IMAGE_BASE_W500
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SeasonBriefDto(
    val id: Int,
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("episode_count") val episodeCount: Int = 0,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("poster_path") val posterPath: String? = null,
)

@Serializable
internal data class SeasonDto(
    val id: Int,
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("episode_count") val episodeCount: Int? = null,
    val episodes: List<EpisodeDto> = emptyList(),
    val name: String? = null,
    val overview: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("poster_path") val posterPath: String? = null,
) {
    fun toSeason(): Season {
        val releaseDate = airDate.parseToEpochMillis()
        val isReleased = releaseDate != null && releaseDate <= System.currentTimeMillis()
        val mappedEpisodes = episodes.map { it.toEpisode() }
        return Season(
            id = id.toString(),
            number = seasonNumber,
            episodes = mappedEpisodes,
            isReleased = isReleased,
            title = name,
            episodeCount = episodeCount ?: mappedEpisodes.size,
            releaseDate = releaseDate,
            overview = overview,
            rating = voteAverage,
            image = posterPath?.let { "$TMDB_IMAGE_BASE_W500$it" },
        )
    }
}
