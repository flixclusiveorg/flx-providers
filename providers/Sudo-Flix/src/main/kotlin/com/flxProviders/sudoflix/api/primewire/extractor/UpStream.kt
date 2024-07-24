package com.flxProviders.sudoflix.api.primewire.extractor

import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.MediaLink
import com.flixclusive.model.provider.Stream
import com.flixclusive.provider.extractor.Extractor
import com.flxProviders.sudoflix.api.primewire.util.getRedirectedUrl
import com.flxProviders.sudoflix.api.util.JsUnpacker
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import java.net.URL

internal class UpStream(
    client: OkHttpClient
) : Extractor(client) {
    override val baseUrl = "https://upstream.to"
    override val name = "Upstream"

    private val packedRegex = Regex("""(eval\(function\(p,a,c,k,e,d\).*\)\)\))""")
    private val linkRegex = Regex("""sources:\[\{file:"(.*?)"""")

    override suspend fun extract(
        url: String,
        customHeaders: Map<String, String>?
    ): List<MediaLink> {
        val embedUrl = getEmbedUrl(url = URL(url))

        val streamPage = client.request(
            url = embedUrl,
            headers = mapOf(
                "Referer" to "https://primewire.tf/",
            ).toHeaders()
        ).execute()
            .body?.string()
            ?: throw Exception("[$name]> Failed to load page")

        val packed = packedRegex.find(streamPage)
            ?.groupValues?.get(1)
            ?: throw Exception("[$name]> Failed to find packed script")

        val unpacked = JsUnpacker(packed).unpack()
            ?: throw Exception("[$name]> Failed to unpack script")

        val link = linkRegex.find(unpacked)
            ?.groupValues?.get(1)
            ?: throw Exception("[$name]> Failed to find packed script")

        return listOf(
            Stream(
                url = link,
                name = name
            )
        )
    }

    private fun getEmbedUrl(url: URL): String {
        return when {
            !url.host.contains("upstream", true) -> {
                val redirectedUrl = getRedirectedUrl(
                    client = client,
                    url = url.toString()
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