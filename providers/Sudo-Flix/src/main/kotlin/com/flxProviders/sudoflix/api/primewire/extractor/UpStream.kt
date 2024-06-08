package com.flxProviders.sudoflix.api.primewire.extractor

import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.provider.extractor.Extractor
import com.flxProviders.sudoflix.api.primewire.util.getRedirectedUrl
import com.flxProviders.sudoflix.api.util.JsUnpacker
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import java.net.URL
import kotlin.random.Random

internal class UpStream(
    client: OkHttpClient
) : Extractor(client) {
    override val baseUrl: String
        get() = "https://upstream.to"
    override val name: String
        get() = "Upstream"

    private val packedRegex = Regex("""(eval\(function\(p,a,c,k,e,d\).*\)\)\))""")
    private val linkRegex = Regex("""sources:\[\{file:"(.*?)"""")

    private fun getRandomUserAgent(): String {
        val osPlatforms = listOf(
            "Windows NT ${Random.nextInt(6, 11)}.0; Win64; x64",
            "Macintosh; Intel Mac OS X 10_${Random.nextInt(8, 16)}_${Random.nextInt(0, 8)}",
            "X11; Ubuntu; Linux x86_64",
            "Linux; Android ${Random.nextInt(7, 12)}",
            "iPhone; CPU iPhone OS ${Random.nextInt(10, 15)}_${Random.nextInt(0, 6)} like Mac OS X"
        )

        val browsers = listOf(
            "Chrome/${Random.nextInt(80, 100)}.0.${Random.nextInt(4000, 5000)}.${Random.nextInt(100, 200)}",
            "Firefox/${Random.nextInt(70, 90)}.0",
            "Safari/605.1.${Random.nextInt(10, 30)}",
            "Edge/${Random.nextInt(16, 20)}.${Random.nextInt(10000, 20000)}"
        )

        val webKits = listOf(
            "AppleWebKit/${Random.nextInt(537, 540)}.36 (KHTML, like Gecko)",
            "AppleWebKit/605.1.${Random.nextInt(10, 30)} (KHTML, like Gecko)"
        )

        val platform = osPlatforms.random()
        val browser = browsers.random()
        val webKit = if (browser.contains("Chrome") || browser.contains("Safari") || browser.contains("Edge")) webKits.random() else ""

        return when {
            browser.contains("Chrome") -> "Mozilla/5.0 ($platform) $webKit $browser Safari/537.36"
            browser.contains("Safari") -> "Mozilla/5.0 ($platform) $webKit $browser"
            browser.contains("Firefox") -> "Mozilla/5.0 ($platform; rv:${browser.split("/")[1]}) Gecko/20100101 $browser"
            browser.contains("Edge") -> "Mozilla/5.0 ($platform) $webKit Edge/${browser.split("/")[1]}"
            else -> "Mozilla/5.0 ($platform)"
        }
    }

    override suspend fun extract(
        url: String,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
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

        onLinkLoaded(
            SourceLink(
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