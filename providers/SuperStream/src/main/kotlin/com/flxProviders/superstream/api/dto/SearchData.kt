package com.flxProviders.superstream.api.dto

import com.google.gson.annotations.SerializedName

internal const val ITEMS_PER_PAGE = 20

internal data class SearchData(
    val data: Result,
) {
    data class Result(
        @SerializedName("docs") val results: List<Item>
    )

    data class Item(
        @SerializedName("id") private val rawId: String,
        @SerializedName("imdb") val imdbId: String
    ) {
        val id: String?
            get() = rawId.split("_").lastOrNull()
    }
}