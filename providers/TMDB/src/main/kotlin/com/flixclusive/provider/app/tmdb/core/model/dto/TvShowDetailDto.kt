package com.flixclusive.provider.app.tmdb.core.model.dto

import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.provider.app.tmdb.core.config.ImageConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TvShowDetailDto(
    val id: Int,
    val name: String,
    val overview: String? = null,
    val tagline: String? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int = 0,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int = 0,
    val seasons: List<SeasonBriefDto> = emptyList(),
    val adult: Boolean = false,
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("last_air_date") val lastAirDate: String? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val homepage: String? = null,
    val genres: List<GenreDto> = emptyList(),
    val networks: List<CompanyDto> = emptyList(),
    @SerialName("external_ids") val externalIds: ExternalIdsDto? = null,
    val credits: CreditsDto? = null,
    val recommendations: RecommendationsDto? = null,
    val images: ImagesDto? = null,
    val keywords: TvKeywordsWrapper? = null,
) {
    fun toShow(providerId: String, seasons: List<Season>, imgCfg: ImageConfig = ImageConfig()): Show {
        val extIds = externalIds?.toExternalIds()?.toMutableMap() ?: mutableMapOf()
        extIds[MediaIdSource.TMDB] = id.toString()

        val genreItems = genres.map { it.toGenre(providerId, isMovie = false) }
        val keywordItems = keywords?.results?.map { it.toGenre(providerId, isMovie = false) } ?: emptyList()
        val allGenres = if (genreItems.size >= 7) genreItems
            else genreItems + keywordItems.take(7 - genreItems.size)

        return Show(
            id = id.toString(),
            providerId = providerId,
            title = name,
            posterImage = posterPath?.let { "${imgCfg.posterDetailBase}$it" },
            backdropImage = backdropPath?.let { "${imgCfg.backdropDetailBase}$it" },
            logoImage = images?.findBestLogo(),
            overview = overview,
            tagLine = tagline,
            runtime = episodeRunTime.firstOrNull(),
            adult = adult,
            language = originalLanguage,
            rating = voteAverage,
            releaseDate = firstAirDate.parseToEpochMillis(),
            lastAirDate = lastAirDate.parseToEpochMillis(),
            homePage = homepage,
            externalIds = extIds,
            genres = allGenres,
            casts = credits?.cast?.map { it.toCast() } ?: emptyList(),
            networks = networks.map { it.toCompany(providerId, isMovie = false) },
            recommendations = recommendations?.results?.mapNotNull {
                it.toPartialMedia(providerId, "tv", imgCfg)
            } ?: emptyList(),
            seasons = seasons,
            totalSeasons = numberOfSeasons,
            totalEpisodes = numberOfEpisodes,
        )
    }
}
