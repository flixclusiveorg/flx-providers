package com.flxProviders.sudoflix.api.primewire.extractor

import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.MediaLink
import com.flixclusive.model.provider.Stream
import com.flixclusive.provider.extractor.Extractor
import com.flxProviders.sudoflix.api.util.JsUnpacker
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.net.URL

internal class MixDrop(
    client: OkHttpClient
) : Extractor(client) {
    override val baseUrl = "https://mixdrop.ag"
    override val name = "MixDrop"

    private val packedRegex = Regex("""(eval\(function\(p,a,c,k,e,d\)\{.*\}\)\))""")
    private val linkRegex = Regex("""MDCore\.wurl="(.*?)";""")

    override suspend fun extract(
        url: String,
        customHeaders: Map<String, String>?
    ): List<MediaLink> {
        val cleanedUrl = getProperMixDropUrl(url)
        val probableHost = cleanedUrl.host
        val embedId = cleanedUrl.pathSegments
            .lastOrNull()
            ?: throw Exception("[$name]> Failed to find embed id")

        val streamPage = client.request(
            url = "https://$probableHost/e/$embedId"
        ).execute()
            .body?.string()
            ?: throw Exception("[$name]> Failed to load embed page")

        val packed = packedRegex.find(streamPage)
            ?.groupValues?.get(1)
            ?: throw Exception("[$name]> Failed to find packed script")

        val unpacked = JsUnpacker(packed).unpack()
            ?: throw Exception("[$name]> Failed to unpack script")

        var link = linkRegex.find(unpacked)
            ?.groupValues?.get(1)
            ?: throw Exception("[$name]> Failed to find link from unpacked script")

        link = if (link.startsWith("http")) {
            link
        } else {
            "https:$link"
        }

        return listOf(
            Stream(
                url = link,
                name = name
            )
        )
    }

    private fun getProperMixDropUrl(url: String): HttpUrl {
        return if (!URL(url).host.contains("mixdrop")) {
            client.request(
                url = url
            ).execute()
                .use {
                    if (!it.isSuccessful) {
                        throw Exception("[$name]> Failed to load page")
                    }

                    it.request.url
                }
        } else url.toHttpUrlOrNull()
            ?: throw Exception("[$name]> Failed to parse URL: $url")
    }
}