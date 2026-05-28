package com.flixclusive.provider.app.tmdb.core.model.dto

import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.provider.app.tmdb.core.config.TMDB_IMAGE_BASE_W500
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EpisodeDto(
    val id: Int,
    @SerialName("episode_number") val episodeNumber: Int,
    @SerialName("season_number") val seasonNumber: Int,
    val name: String? = null,
    val overview: String? = null,
    val runtime: Int? = null,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("still_path") val stillPath: String? = null,
) {
    fun toEpisode(): Episode {
        val releaseDate = airDate.parseToEpochMillis()
        val isReleased = releaseDate != null && releaseDate <= System.currentTimeMillis()
        return Episode(
            id = id.toString(),
            number = episodeNumber,
            season = seasonNumber,
            isReleased = isReleased,
            title = name,
            overview = overview,
            runtime = runtime,
            releaseDate = releaseDate,
            image = stillPath?.let { "$TMDB_IMAGE_BASE_W500$it" },
            rating = voteAverage,
        )
    }
}
