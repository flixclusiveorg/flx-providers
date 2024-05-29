package com.flxProviders.sudoflix.api.nsbx.util

import com.flxProviders.sudoflix.api.nsbx.NsbxConstant.TMDB_API_KEY

internal fun getTmdbQuery(
    id: String,
    filmType: String
): String {
    return "https://api.themoviedb.org/3/$filmType/$id?api_key=$TMDB_API_KEY&append_to_response=external_ids"
}