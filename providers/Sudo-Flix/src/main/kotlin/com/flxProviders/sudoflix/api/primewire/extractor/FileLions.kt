package com.flxProviders.sudoflix.api.primewire.extractor

import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.provider.extractor.Extractor
import okhttp3.OkHttpClient

internal class FileLions(
    client: OkHttpClient
) : Extractor(client) {
    override val baseUrl: String
        get() = "https://filelions.site"
    override val name: String
        get() = "FileLions"

    private val linkRegex = Regex("""file: ?"(http.*?)"""")

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
            ?: throw Exception("[$name]> Stream URL not found in embed code")

        onLinkLoaded(
            SourceLink(
                name = name,
                url = link
            )
        )
    }
}