package com.flxProviders.sudoflix.api.primewire.extractor

import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.MediaLink
import com.flixclusive.model.provider.Stream
import com.flixclusive.provider.extractor.Extractor
import okhttp3.OkHttpClient

internal class StreamWish(
    client: OkHttpClient
) : Extractor(client) {
    override val baseUrl = "https://streamwish.to"
    override val name = "StreamWish"

    private val linkRegex = Regex("""file:"(https://[^"]+)"""")

    override suspend fun extract(
        url: String,
        customHeaders: Map<String, String>?
    ): List<MediaLink> {
        val streamPage = client.request(url = url)
            .execute().body?.string()
            ?: throw Exception("[$name]> Failed to load page")

        val link = linkRegex.find(streamPage)
            ?.groupValues?.get(1)
            ?: throw Exception("[$name]> Failed to find link")

        return listOf(
            Stream(
                url = link,
                name = name
            )
        )
    }
}