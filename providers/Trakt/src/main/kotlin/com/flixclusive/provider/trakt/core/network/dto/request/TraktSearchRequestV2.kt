package com.flixclusive.provider.trakt.core.network.dto.request


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TraktSearchRequestV2(
    @SerialName("union")
    val union: Boolean = true,
    @SerialName("searches")
    val searches: List<SearchRequest>
) {
    @Serializable
    data class SearchRequest(
        @SerialName("collection") val collection: String,
        @SerialName("preset") val preset: String = "search:media"
    )
}