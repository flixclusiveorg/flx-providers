package com.flxProviders.sudoflix.api.util

import com.google.gson.annotations.SerializedName

internal const val TMDB_API_KEY = "8d6d91941230817f7807d643736e8a49"

internal fun getTmdbQuery(
    id: String,
    filmType: String
): String {
    return "https://api.themoviedb.org/3/$filmType/$id?api_key=$TMDB_API_KEY&append_to_response=external_ids"
}

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