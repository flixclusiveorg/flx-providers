package com.flxProviders.superstream.api.dto

import com.google.gson.annotations.SerializedName

internal data class TmdbQueryDto(
    @SerializedName("imdb_id") val imdbId: String? = null,
    @SerializedName("external_ids") val externalIds: Map<String, Any>
)