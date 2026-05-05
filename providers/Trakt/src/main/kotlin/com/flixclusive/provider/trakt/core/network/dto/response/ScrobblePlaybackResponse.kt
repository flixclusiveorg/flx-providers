package com.flixclusive.provider.trakt.core.network.dto.response

import com.flixclusive.provider.trakt.core.network.dto.request.ScrobbleEpisode
import com.flixclusive.provider.trakt.core.network.dto.request.ScrobbleMedia
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class ScrobblePlaybackResponse(
    @JsonNames("movie", "show") val media: ScrobbleMedia,
    @SerialName("progress") val progress: Float,
    @SerialName("episode") val episode: ScrobbleEpisode? = null,
) {
    val id get() = media.ids.id
}