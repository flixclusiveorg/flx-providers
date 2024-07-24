package com.flxProviders.sudoflix.api.primewire.extractor

import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.MediaLink
import com.flixclusive.model.provider.Stream
import com.flixclusive.provider.extractor.Extractor
import okhttp3.OkHttpClient

internal class FileLions(
    client: OkHttpClient
) : Extractor(client) {
    override val baseUrl = "https://filelions.site"
    override val name = "FileLions"

    private val linkRegex = Regex("""file: ?"(http.*?)"""")

    override suspend fun extract(
        url: String,
        customHeaders: Map<String, String>?
    ): List<MediaLink> {
        val streamPage = client.request(url = url)
            .execute().body?.string()
            ?: throw Exception("[$name]> Failed to load page")

        val link = linkRegex.find(streamPage)
            ?.groupValues?.get(1)
            ?: throw Exception("[$name]> Stream URL not found in embed code")

        return listOf(
            Stream(
                name = name,
                url = link
            )
        )
    }
}