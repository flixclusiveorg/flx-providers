package com.flixclusive.provider.trakt.core.network.dto.response

import com.flixclusive.provider.trakt.core.model.TraktMedia
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class TraktSearchMediaItemResponse(
    @JsonNames("movie", "show") val media: TraktMedia,
    @SerialName("score") val score: Int = 0,
    @SerialName("type") val type: String
) {
    val id: String get() = media.id

    val isMovie get() = type == "movie"
    val isShow get() = type == "show"

}