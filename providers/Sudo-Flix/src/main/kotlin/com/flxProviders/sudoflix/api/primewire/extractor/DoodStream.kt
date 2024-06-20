package com.flxProviders.sudoflix.api.primewire.extractor

import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.provider.extractor.Extractor
import com.flxProviders.sudoflix.api.primewire.util.getRedirectedUrl
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import java.net.URL
import kotlin.random.Random

internal class DoodStream(
    client: OkHttpClient
) : Extractor(client) {
    override val baseUrl: String
        get() = "https://dood.re"
    override val name: String
        get() = "DoodStream"

    private val tokenRegex = Regex("""\?token=([^&]+)&expiry=""")
    private val pathRegex = Regex("""\$.get\('/pass_md5([^']+)""")

    override suspend fun extract(
        url: String,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
        val embedRawUrl = getEmbedUrl(url)
        val embedUrl = embedRawUrl.toString()
        val embedId = embedUrl.split("/d/").lastOrNull()
            ?: embedUrl.split("/e/").lastOrNull()
            ?: throw Exception("[$name]> No embed id")

        val finalEmbedUrl = "https://${embedRawUrl.host}/e/$embedId"
        val page = client.request(
            url = finalEmbedUrl
        ).execute().body?.string()
            ?: throw Exception("[$name]> Failed to load page")

        val token = tokenRegex.find(page)
            ?.groupValues?.get(1)
            ?: throw Exception("[$name]> Token not found")

        val path = pathRegex.find(page)
            ?.groupValues?.get(1)
            ?: throw Exception("[$name]> Path not found")

        val nextPage = client.request(
            url = "https://${embedRawUrl.host}/pass_md5$path",
            headers = mapOf(
                "Referer" to finalEmbedUrl,
            ).toHeaders()
        ).execute().body?.string()
            ?: throw Exception("[$name]> Failed to load next page")

        val eightHoursInMillis = 8 * 60 * 60 * 1000
        val expiry = System.currentTimeMillis() + eightHoursInMillis

        val downloadUrl = "${nextPage}${generateRandomToken()}?token=${token}&expiry=${expiry}"

        onLinkLoaded(
            SourceLink(
                url = downloadUrl,
                name = name,
                customHeaders = mapOf(
                    "Referer" to "$baseUrl/"
                )
            )
        )
    }

    private fun getEmbedUrl(url: String): URL {
        return when(URL(url).host.contains("dood", true)) {
            false -> {
                val redirectedUrl = getRedirectedUrl(
                    client = client,
                    url = url
                )
                getEmbedUrl(redirectedUrl)
            }
            else -> URL(url)
        }
    }

    private fun generateRandomToken(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = Random(System.currentTimeMillis())

        return (1..10)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}