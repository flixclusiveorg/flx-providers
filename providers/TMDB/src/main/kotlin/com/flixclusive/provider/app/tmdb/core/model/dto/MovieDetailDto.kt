package com.flixclusive.provider.app.tmdb.core.model.dto

import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.MovieCollection
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.provider.app.tmdb.core.config.ImageConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MovieDetailDto(
    val id: Int,
    val title: String,
    val overview: String? = null,
    val tagline: String? = null,
    val runtime: Int? = null,
    val adult: Boolean = false,
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val homepage: String? = null,
    val genres: List<GenreDto> = emptyList(),
    @SerialName("production_companies") val productionCompanies: List<CompanyDto> = emptyList(),
    @SerialName("belongs_to_collection") val belongsToCollection: CollectionBriefDto? = null,
    @SerialName("external_ids") val externalIds: ExternalIdsDto? = null,
    val credits: CreditsDto? = null,
    val recommendations: RecommendationsDto? = null,
    val images: ImagesDto? = null,
    val keywords: MovieKeywordsWrapper? = null,
) {
    fun toMovie(providerId: String, imgCfg: ImageConfig = ImageConfig()): Movie {
        val extIds = externalIds?.toExternalIds()?.toMutableMap() ?: mutableMapOf()
        extIds[MediaIdSource.TMDB] = id.toString()

        val genreItems = genres.map { it.toGenre(providerId, isMovie = true) }
        val keywordItems = keywords?.keywords?.map { it.toGenre(providerId, isMovie = true) } ?: emptyList()
        val allGenres = if (genreItems.size >= 7) genreItems
            else genreItems + keywordItems.take(7 - genreItems.size)

        return Movie(
            id = id.toString(),
            providerId = providerId,
            title = title,
            posterImage = posterPath?.let { "${imgCfg.posterDetailBase}$it" },
            backdropImage = backdropPath?.let { "${imgCfg.backdropDetailBase}$it" },
            logoImage = images?.findBestLogo(),
            overview = overview,
            tagLine = tagline,
            runtime = runtime,
            adult = adult,
            language = originalLanguage,
            rating = voteAverage,
            releaseDate = releaseDate.parseToEpochMillis(),
            homePage = homepage,
            externalIds = extIds,
            genres = allGenres,
            casts = credits?.cast?.map { it.toCast() } ?: emptyList(),
            producers = productionCompanies.map { it.toCompany(providerId, isMovie = true) },
            recommendations = recommendations?.results?.mapNotNull {
                it.toPartialMedia(providerId, "movie", imgCfg)
            } ?: emptyList(),
            collection = belongsToCollection?.let {
                MovieCollection(
                    name = it.name,
                    posterImage = it.posterPath?.let { p -> "${imgCfg.posterDetailBase}$p" },
                    backdropImage = it.backdropPath?.let { p -> "${imgCfg.backdropDetailBase}$p" },
                    overview = it.overview,
                    parts = emptyList(),
                )
            },
        )
    }
}
