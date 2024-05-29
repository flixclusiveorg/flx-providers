package com.flxProviders.sudoflix.api.nsbx.dto

import com.google.gson.annotations.SerializedName

internal data class TmdbQueryDto(
    @SerializedName("imdb_id") val imdbId: String? = null,
    @SerializedName("id") val tmdbId: String,
    @SerializedName("original_title", alternate = ["original_name"]) val title: String,
    @SerializedName("release_date", alternate = ["first_air_date"]) val releaseDate: String,
    @SerializedName("external_ids") val externalIds: Map<String, Any>
) {
    val releaseYear: Int
        get() = getYearFromString(releaseDate)

    private fun getYearFromString(dateString: String): Int {
        val parts = dateString.split("-")
        return parts[0].toInt()
    }
}