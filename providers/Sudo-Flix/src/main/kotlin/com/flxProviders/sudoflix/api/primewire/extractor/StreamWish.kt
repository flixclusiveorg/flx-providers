package com.flxProviders.sudoflix.api.primewire.extractor

import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.provider.extractor.Extractor
import okhttp3.OkHttpClient

internal class StreamWish(
    client: OkHttpClient
) : Extractor(client) {
    override val baseUrl: String
        get() = "https://streamwish.to"
    override val name: String
        get() = "StreamWish"

    private val linkRegex = Regex("""file:"(https://[^"]+)"""")

    override suspend fun extract(
        url: String,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
        val streamPage = client.request(url = url)
            .execute().body?.string()
            ?: throw Exception("[$name]> Failed to load page")

        val link = linkRegex.find(streamPage)
            ?.groupValues?.get(1)
            ?: throw Exception("[$name]> Failed to find link")

        onLinkLoaded(
            SourceLink(
                url = link,
                name = name
            )
        )
    }
}