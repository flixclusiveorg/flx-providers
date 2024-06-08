package com.flxProviders.sudoflix.api.primewire.extractor

import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.provider.extractor.Extractor
import com.flxProviders.sudoflix.api.util.JsUnpacker
import okhttp3.OkHttpClient

internal class StreamVid(
    client: OkHttpClient
) : Extractor(client) {
    override val baseUrl: String
        get() = "https://streamvid.net"
    override val name: String
        get() = "StreamVid"

    private val packedRegex = Regex("""eval\((function\(p,a,c,k,e,d\)\{.*)\)""")
    private val linkRegex = Regex("""src:"(https://[^"]+)"""")

    override suspend fun extract(
        url: String,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
        val streamPage = client.request(url = url)
            .execute().body?.string()
            ?: throw Exception("[$name]> Failed to load page")

        val packed = packedRegex.find(streamPage)
            ?.groupValues?.get(1)
            ?: throw Exception("[$name]> Failed to find packed script")

        val unpacked = JsUnpacker(packed).unpack()
            ?: throw Exception("[$name]> Failed to unpack script")

        val link = linkRegex.find(unpacked)
            ?.groupValues?.get(1)
            ?: throw Exception("[$name]> Failed to find link from unpacked script")

        onLinkLoaded(
            SourceLink(
                url = link,
                name = name
            )
        )
    }
}