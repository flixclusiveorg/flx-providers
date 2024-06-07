package com.flxProviders.sudoflix.api.primewire.extractor

import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.provider.extractor.Extractor
import okhttp3.OkHttpClient
import java.net.URL

internal class StreamWish(
    private val client: OkHttpClient
) : Extractor() {
    override val host: String
        get() = "https://streamwish.to"
    override val name: String
        get() = "StreamWish"

    private val linkRegex = Regex("""file:"(https://[^"]+)"""")

    override suspend fun extract(
        url: URL,
        mediaId: String,
        episodeId: String,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
        val streamPage = client.request(
            url = url.toString()
        ).execute()
            .body?.string()
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