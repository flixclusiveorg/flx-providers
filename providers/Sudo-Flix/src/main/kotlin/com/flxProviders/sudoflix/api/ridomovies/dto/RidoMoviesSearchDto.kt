package com.flxProviders.sudoflix.api.ridomovies.dto

import com.google.gson.annotations.SerializedName

internal data class RidoMoviesSearchDto(
    val data: RidoMoviesSearchData
)

internal data class RidoMoviesSearchData(
    val items: List<RidoMoviesSearchItem>
)

internal data class RidoMoviesSearchItem(
    val fullSlug: String,
    @SerializedName("contentable") val contentTable: RidoMoviesSearchItemContentTable
)

internal data class RidoMoviesSearchItemContentTable(
    val tmdbId: Int,
    val imdbId: String
)

internal data class RidoMoviesPaginationData(
    val pageNumber: Int,
    val hasNext: Int,
)