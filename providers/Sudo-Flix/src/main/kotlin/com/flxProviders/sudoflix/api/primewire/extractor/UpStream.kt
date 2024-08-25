package com.flxProviders.sudoflix.api.primewire.extractor

import com.flixclusive.model.provider.MediaLink
import com.flixclusive.provider.extractor.Extractor
import com.flxProviders.sudoflix.api.primewire.util.ExtractorHelper.getRedirectedUrl
import com.flxProviders.sudoflix.api.primewire.util.ExtractorHelper.unpackLinks
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import java.net.URL

internal class UpStream(
    client: OkHttpClient
) : Extractor(client) {
    override val baseUrl = "https://upstream.to"
    override val name = "Upstream"

    override suspend fun extract(
        url: String,
        customHeaders: Map<String, String>?
    ): List<MediaLink> {
        val embedUrl = getEmbedUrl(url = URL(url))

        return unpackLinks(
            client = client,
            url = embedUrl,
            headers = mapOf(
                "Referer" to "https://primewire.tf/",
            ).toHeaders(),
            linkRegex = Regex("""sources:\[\{file:"(.*?)"""")
        )
    }

    private fun getEmbedUrl(url: URL): String {
        return when {
            !url.host.contains("upstream", true) -> {
                val redirectedUrl = getRedirectedUrl(
                    client = client,
                    url = url.toString(),
                    domainName = "upstream"
                )
                getEmbedUrl(URL(redirectedUrl))
            }
            !url.path.contains("embed", true) -> {
                val embedId = url.path.split("/").last()

                "https://${url.host}/embed-$embedId.html"
            }
            else -> url.toString()
        }
    }
}