package com.flxProviders.superstream.api.dto

import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.util.FilmType
import com.flxProviders.superstream.BuildConfig.SUPERSTREAM_FIRST_API
import com.google.gson.annotations.SerializedName

internal const val ITEMS_PER_PAGE = 20


internal data class SearchData(
    @SerializedName("data") val results: List<SearchItem>,
    val total: Int = 0,
)

internal data class SearchItem(
    @SerializedName("audio_lang") val language: String? = null,
    @SerializedName("imdb_rating") private val imdbRating: String? = null,
    @SerializedName("title") val name: String,
    @SerializedName("box_type") val boxType: Int,
    val id: Int,
    val poster: String? = null,
    val year: Int? = null,
) {
    private val rating: Double
        get() = imdbRating?.toDoubleOrNull() ?: 0.0

    private val filmType: FilmType
        get() = when (boxType) {
            BoxType.Series.value -> FilmType.TV_SHOW
            BoxType.Movies.value -> FilmType.MOVIE
            else -> throw IllegalArgumentException("[SuperStream]> Unknown film type: $boxType")
        }

    private val homePage: String
        get() = "$SUPERSTREAM_FIRST_API/${filmType.type}/$id"

    fun toFilmSearchItem(
        provider: String,
        imdbId: String?,
        tmdbId: Int?,
    ) = FilmSearchItem(
        id = id.toString(),
        tmdbId = tmdbId,
        providerName = provider,
        imdbId = imdbId,
        title = name,
        rating = rating,
        year = year,
        homePage = homePage,
        posterImage = poster,
        filmType = filmType,
    )
}
