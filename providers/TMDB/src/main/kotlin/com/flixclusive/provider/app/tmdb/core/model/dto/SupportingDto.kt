package com.flixclusive.provider.app.tmdb.core.model.dto

import androidx.compose.ui.util.fastFirstOrNull
import com.flixclusive.model.media.common.Cast
import com.flixclusive.model.media.common.Company
import com.flixclusive.model.media.common.Genre
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.provider.Catalog
import com.flixclusive.provider.app.tmdb.core.config.TMDB_API_BASE_URL
import com.flixclusive.provider.app.tmdb.core.config.TMDB_IMAGE_BASE_W500
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GenreDto(
    val id: Int,
    val name: String,
)

@Serializable
internal data class KeywordDto(
    val id: Int,
    val name: String,
)

@Serializable
internal data class CompanyDto(
    val id: Int,
    val name: String,
    @SerialName("logo_path") val logoPath: String? = null,
)

@Serializable
internal data class ExternalIdsDto(
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("tvdb_id") val tvdbId: Int? = null,
)

@Serializable
internal data class ImageFileDto(
    @SerialName("file_path") val filePath: String,
    @SerialName("iso_639_1") val language: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
)

@Serializable
internal data class ImagesDto(
    val backdrops: List<ImageFileDto> = emptyList(),
    val posters: List<ImageFileDto> = emptyList(),
    val logos: List<ImageFileDto> = emptyList(),
)

@Serializable
internal data class CastDto(
    val id: Int,
    val name: String,
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
)

@Serializable
internal data class CreditsDto(
    val cast: List<CastDto> = emptyList(),
)

@Serializable
internal data class CollectionBriefDto(
    val id: Int,
    val name: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val overview: String? = null,
)

@Serializable
internal data class RecommendationsDto(
    val page: Int = 1,
    val results: List<FilmSearchItemDto> = emptyList(),
)

@Serializable
internal data class MovieKeywordsWrapper(
    val keywords: List<KeywordDto> = emptyList(),
)

@Serializable
internal data class TvKeywordsWrapper(
    val results: List<KeywordDto> = emptyList(),
)

internal fun GenreDto.toGenre(providerId: String, isMovie: Boolean): Genre {
    val mediaPath = if (isMovie) "movie" else "tv"
    val dateParam = if (isMovie) "primary_release_date" else "first_air_date"
    val discoverUrl = "${TMDB_API_BASE_URL}discover/$mediaPath" +
        "?include_video=false" +
        "&sort_by=popularity.desc&${dateParam}.gte=1990-01-01" +
        "&vote_count.gte=200&with_genres=$id"
    return Genre(
        name = name,
        catalog = Catalog(
            name = name,
            url = discoverUrl,
            providerId = providerId,
            canPaginate = true,
        ),
    )
}

internal fun KeywordDto.toGenre(providerId: String, isMovie: Boolean): Genre {
    val mediaPath = if (isMovie) "movie" else "tv"
    val dateParam = if (isMovie) "primary_release_date" else "first_air_date"
    val discoverUrl = "${TMDB_API_BASE_URL}discover/$mediaPath" +
        "?include_video=false" +
        "&sort_by=popularity.desc&${dateParam}.gte=1990-01-01" +
        "&vote_count.gte=200&with_keywords=$id"
    return Genre(
        name = name,
        catalog = Catalog(
            name = name,
            url = discoverUrl,
            providerId = providerId,
            canPaginate = true,
        ),
    )
}

internal fun CompanyDto.toCompany(providerId: String, isMovie: Boolean): Company {
    val mediaPath = if (isMovie) "movie" else "tv"
    val dateParam = if (isMovie) "primary_release_date" else "first_air_date"
    val withParam = if (isMovie) "with_companies" else "with_networks"
    val discoverUrl = "${TMDB_API_BASE_URL}discover/$mediaPath" +
        "?include_video=false" +
        "&sort_by=popularity.desc&${dateParam}.gte=1990-01-01" +
        "&vote_count.gte=200&$withParam=$id"
    return Company(
        name = name,
        catalog = Catalog(
            name = name,
            url = discoverUrl,
            providerId = providerId,
            canPaginate = true,
        ),
    )
}

internal fun CastDto.toCast(): Cast = Cast(
    name = name,
    character = character,
    profileImage = profilePath?.let { "$TMDB_IMAGE_BASE_W500$it" },
)

internal fun ExternalIdsDto.toExternalIds(): Map<MediaIdSource, String> {
    val map = mutableMapOf<MediaIdSource, String>()
    imdbId?.takeIf { it.isNotBlank() }?.let { map[MediaIdSource.IMDB] = it }
    tvdbId?.let { map[MediaIdSource.TVDB] = it.toString() }
    return map
}

internal fun ImagesDto.findBestLogo(): String? {
    val enLogo = logos.fastFirstOrNull { it.language == "en" }
    val logo = enLogo ?: logos.firstOrNull() ?: return null
    return "$TMDB_IMAGE_BASE_W500${logo.filePath}".replace(".svg", ".png")
}
