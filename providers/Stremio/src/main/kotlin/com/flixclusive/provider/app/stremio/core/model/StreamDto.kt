package com.flixclusive.provider.app.stremio.core.model

import com.flixclusive.core.util.log.warnLog
import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.Stream
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URLEncoder

/**
 *
 * Based from [Hexated's](https://github.com/hexated/cloudstream-extensions-hexated/blob/master/StremioX/src/main/kotlin/com/hexated/StremioX.kt#L243)
 * */
@Serializable
internal data class StreamDto(
    val url: String? = null,
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val ytId: String? = null,
    val externalUrl: String? = null,
    val infoHash: String? = null,
    val sources: List<String>? = null,
    val subtitles: List<SubtitleDto>? = null,
    @SerialName("behaviorHints") val extraOptions: ExtraOptions? = null
) {
    fun toStreamLink(): Stream? {
        val streamUrl = url ?: toMagnet() ?: return null
        val headers = (extraOptions?.headers ?: emptyMap())
            .plus(extraOptions?.proxyHeaders?.request ?: emptyMap())

        return Stream(
            url = streamUrl,
            name = fixSourceName(
                name = name,
                description = description,
                title = title
            ),
            flags = setOf(
                Flag.RequiresAuth(customHeaders = headers),
            )
        )
    }

    private fun toMagnet(): String? {
        val hash = infoHash?.takeIf { it.isNotBlank() } ?: return null
        val trackers = sources.orEmpty()
            .filter { it.startsWith("tracker:") }
            .map { it.removePrefix("tracker:") }
            .filter { "://" in it }
            .distinct()

        if (hash.startsWith("magnet:")) {
            warnLog("[Stremio] infoHash might be invalid: $hash")
        }

        return buildString {
            append("magnet:?xt=urn:btih:").append(hash)
            extraOptions?.filename?.let { append("&dn=").append(URLEncoder.encode(it, "UTF-8")) }
            trackers.forEach { append("&tr=").append(URLEncoder.encode(it, "UTF-8")) }
        }
    }

    private fun fixSourceName(
        name: String?,
        description: String?,
        title: String?
    ): String {
        return when {
            name?.contains("[RD+]", true) == true -> "[RD+] $title".trim()
            name?.contains("[RD download]", true) == true -> "[RD download] $title".trim()
            else -> "${name?.trimIndent() ?: ""}\n${title?.trimIndent() ?: ""}\n${description?.trimIndent() ?: ""}"
        }
    }
}

@Serializable
internal data class StreamResponse(
    val streams: List<StreamDto>,
    override val err: String? = null,
) : CommonErrorResponse()

@Serializable
internal data class ProxyHeaders(
    val request: Map<String, String>? = null,
)

@Serializable
internal data class ExtraOptions(
    val proxyHeaders: ProxyHeaders? = null,
    val headers: Map<String, String>? = null,
    val filename: String? = null,
)