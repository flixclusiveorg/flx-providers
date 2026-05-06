package com.flixclusive.provider.app.trakt.core.network.dto.request

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.provider.app.trakt.core.model.TraktGenericIdMap
import com.flixclusive.provider.app.trakt.core.model.TraktGenericIdMap.Companion.toTraktIds
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ScrobbleRequest(
    @SerialName("movie") val movie: ScrobbleMedia? = null,
    @SerialName("show") val show: ScrobbleMedia? = null,
    @SerialName("episode") val episode: ScrobbleEpisode? = null,
    @SerialName("progress") val progress: Float,
)

@Serializable
internal data class ScrobbleEpisode(
    @SerialName("number") val number: Int,
    @SerialName("season") val season: Int,
) {
    companion object {
        fun Episode.toScrobbleEpisode(): ScrobbleEpisode {
            return ScrobbleEpisode(
                number = number,
                season = season,
            )
        }
    }
}

@Serializable
internal data class ScrobbleMedia(
    @SerialName("ids") val ids: TraktGenericIdMap,
    @SerialName("title") val title: String,
) {
    companion object {
        fun MediaMetadata.toScrobbleMedia(): ScrobbleMedia {
            return ScrobbleMedia(
                ids = toTraktIds(),
                title = title,
            )
        }
    }
}