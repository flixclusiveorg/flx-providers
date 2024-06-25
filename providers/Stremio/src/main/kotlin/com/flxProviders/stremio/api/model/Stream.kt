package com.flxProviders.stremio.api.model

import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flxProviders.stremio.api.util.isValidUrl
import com.google.gson.annotations.SerializedName
import java.net.URL

/**
 *
 * Based from [Hexated's](https://github.com/hexated/cloudstream-extensions-hexated/blob/master/StremioX/src/main/kotlin/com/hexated/StremioX.kt#L243)
 * */
internal data class Stream(
    val url: String?,
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val ytId: String? = null,
    val externalUrl: String? = null,
    val infoHash: String? = null,
    val sources: List<String>? = null,
    val subtitles: List<Subtitle>? = null,
    @SerializedName("behaviorHints") val extraOptions: ExtraOptions? = null
) {
    companion object {
        fun Stream.toSourceLink(): SourceLink? {
            val isValidUrl = isValidUrl(url)
            if (!isValidUrl) return null

            return SourceLink(
                url = URL(url).toString(),
                name = fixSourceName(
                    name = name,
                    description = description,
                    title = title
                ),
                customHeaders = extraOptions?.headers
                    ?.plus(
                    extraOptions.proxyHeaders
                        ?.request
                        ?: emptyMap()
                    )
            )
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
}

internal data class StreamResponse(
    val streams: List<Stream>,
    override val err: String?,
) : CommonErrorResponse()

internal data class ProxyHeaders(
    val request: Map<String, String>?,
)

internal data class ExtraOptions(
    val proxyHeaders: ProxyHeaders?,
    val headers: Map<String, String>?,
)