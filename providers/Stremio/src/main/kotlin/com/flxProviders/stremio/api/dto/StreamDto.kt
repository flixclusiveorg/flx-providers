package com.flxProviders.stremio.api.dto

import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flxProviders.stremio.api.util.isValidUrl
import com.google.gson.annotations.SerializedName
import java.net.URL

/**
 *
 * Based from [Hexated's](https://github.com/hexated/cloudstream-extensions-hexated/blob/master/StremioX/src/main/kotlin/com/hexated/StremioX.kt#L243)
 * */
internal data class StreamDto(
    val name: String?,
    val title: String?,
    val url: String?,
    val description: String?,
    val ytId: String?,
    val externalUrl: String?,
    @SerializedName("behaviorHints") val extraOptions: ExtraOptions?,
    val infoHash: String?,
    val sources: List<String>? = null,
    val subtitles: List<Subtitle>? = null
) {
    companion object {
        fun StreamDto.toSourceLink(): SourceLink? {
            val isValidUrl = isValidUrl(url)
            if (!isValidUrl) return null

            return SourceLink(
                url = URL(url).toString(),
                name = name!!
            )
        }
    }
}

internal data class StreamResponse(
    val streams: List<StreamDto>,
)

internal data class ProxyHeaders(
    val request: Map<String, String>?,
)

internal data class ExtraOptions(
    val proxyHeaders: ProxyHeaders?,
    val headers: Map<String, String>?,
)