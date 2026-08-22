package com.flixclusive.provider.app.letterboxd.core.model.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class ImageSizeDto(
    val width: Int = 0,
    val height: Int = 0,
    val url: String = "",
)

@Serializable
internal data class ImageDto(
    val sizes: List<ImageSizeDto> = emptyList(),
)

@Serializable
internal data class GenreDto(
    val id: String = "",
    val name: String = "",
)

@Serializable
internal data class ContributorSummaryDto(
    val id: String = "",
    val name: String = "",
)

@Serializable
internal data class LinkDto(
    val type: String = "",
    val id: String = "",
    val url: String = "",
)

@Serializable
internal data class TrailerDto(
    val id: String = "",
    val url: String = "",
)

@Serializable
internal data class LanguageDto(
    val code: String = "",
    val name: String = "",
)

/**
 * Covers `FilmSummary`, `Production` and `Film` in one shape. `AppJson` ignores
 * unknown keys, so the summary responses simply leave the detail fields null.
 */
@Serializable
internal data class FilmDto(
    val id: String = "",
    val name: String = "",
    val originalName: String? = null,
    val releaseYear: Int? = null,
    val releaseDate: String? = null,
    val runTime: Int? = null,
    val rating: Double? = null,
    val adult: Boolean = false,
    val poster: ImageDto? = null,
    val adultPoster: ImageDto? = null,
    val backdrop: ImageDto? = null,
    val description: String? = null,
    val tagline: String? = null,
    val trailer: TrailerDto? = null,
    val primaryLanguage: LanguageDto? = null,
    val genres: List<GenreDto> = emptyList(),
    val directors: List<ContributorSummaryDto> = emptyList(),
    val links: List<LinkDto> = emptyList(),
)

@Serializable
internal data class FilmsResponseDto(
    val next: String? = null,
    val items: List<FilmDto> = emptyList(),
)

@Serializable
internal data class SearchItemDto(
    val type: String = "",
    val film: FilmDto? = null,
)

@Serializable
internal data class SearchResponseDto(
    val next: String? = null,
    val items: List<SearchItemDto> = emptyList(),
)
