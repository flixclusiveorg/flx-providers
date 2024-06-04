package com.flxProviders.superstream.api.dto

import com.flixclusive.provider.dto.SearchResultItem
import com.flxProviders.superstream.api.util.SuperStreamUtil
import com.google.gson.annotations.SerializedName

internal data class SuperStreamSearchResponse(
    @SerializedName("data") val results: List<SuperStreamSearchItem>,
    val total: Int = 0,
) {
    data class SuperStreamSearchItem(
        val id: Int? = null,
        @SerializedName("box_type") val boxType: Int? = null,
        val title: String? = null,
        val year: Int? = null,
    ) {
        companion object {
            fun SuperStreamSearchItem.toSearchResultItem(): SearchResultItem {
                return SearchResultItem(
                    id = id.toString(),
                    title = title,
                    releaseDate = year.toString(),
                    filmType = SuperStreamUtil.SSMediaType.getSSMediaType(boxType).toFilmType(),
                )
            }
        }
    }
}