package com.flxProviders.superstream.dto

import com.flixclusive.model.film.FilmSearchItem
import com.flxProviders.superstream.util.SuperStreamUtil
import com.google.gson.annotations.SerializedName

internal data class SearchData(
    @SerializedName("data") val results: List<SearchItem>,
    val total: Int = 0,
)

internal data class SearchItem(
    val id: Int? = null,
    @SerializedName("box_type") val boxType: Int? = null,
    val title: String? = null,
    val year: Int? = null,
) {
    companion object {
        fun SearchItem.toFilmSearchItem(): FilmSearchItem {
            return FilmSearchItem(
                id = id.toString(),
                title = title ?: "UNKNOWN TITLE",
                releaseDate = year.toString(),
                filmType = SuperStreamUtil.SSMediaType.getSSMediaType(boxType).toFilmType(),
                posterImage = null,
                homePage = null,
                providerName = "SuperStream"
            )
        }
    }
}
