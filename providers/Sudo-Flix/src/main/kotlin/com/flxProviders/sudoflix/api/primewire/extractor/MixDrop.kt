package com.flxProviders.sudoflix.api.primewire.extractor

import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.provider.extractor.Extractor
import com.flxProviders.sudoflix.api.util.JsUnpacker
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.net.URL

internal class MixDrop(
    private val client: OkHttpClient
) : Extractor() {
    override val host: String
        get() = "https://mixdrop.ag"
    override val name: String
        get() = "MixDrop"

    private val packedRegex = Regex("""(eval\(function\(p,a,c,k,e,d\)\{.*\}\)\))""")
    private val linkRegex = Regex("""MDCore\.wurl="(.*?)";""")

    override suspend fun extract(
        url: URL,
        mediaId: String,
        episodeId: String,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
        val cleanedUrl = getProperMixDropUrl(url)
        val probableHost = cleanedUrl.host
        val embedId = cleanedUrl.pathSegments
            .lastOrNull()
            ?: throw Exception("[$name]> Failed to find embed id")

        val streamPage = client.request(
            url = "https://$probableHost/e/$embedId"
        ).execute()
            .body?.string()
            ?: throw Exception("[$name]> Failed to load embed page")

        val packed = packedRegex.find(streamPage)
            ?.groupValues?.get(1)
            ?: throw Exception("[$name]> Failed to find packed script")

        val unpacked = JsUnpacker(packed).unpack()
            ?: throw Exception("[$name]> Failed to unpack script")

        var link = linkRegex.find(unpacked)
            ?.groupValues?.get(1)
            ?: throw Exception("[$name]> Failed to find link from unpacked script")

        link = if (link.startsWith("http")) {
            link
        } else {
            "https:$link"
        }

        onLinkLoaded(
            SourceLink(
                url = link,
                name = name
            )
        )
    }

    private fun getProperMixDropUrl(url: URL): HttpUrl {
        return if (!url.host.contains("mixdrop")) {
            client.request(
                url = url.toString()
            ).execute()
                .use {
                    if (!it.isSuccessful) {
                        throw Exception("[$name]> Failed to load page")
                    }

                    it.request.url
                }
        } else url.toHttpUrlOrNull()
            ?: throw Exception("[$name]> Failed to parse URL: $url")
    }
}