package com.flxProviders.superstream.api.util

internal const val TMDB_API_KEY = "8d6d91941230817f7807d643736e8a49"
internal fun getTmdbQuery(
    id: String,
    filmType: String
): String {
    return "https://api.themoviedb.org/3/$filmType/$id?api_key=$TMDB_API_KEY&append_to_response=external_ids"
}