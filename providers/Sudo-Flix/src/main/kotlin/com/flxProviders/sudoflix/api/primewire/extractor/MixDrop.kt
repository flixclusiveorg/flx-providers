package com.flxProviders.sudoflix.api.primewire.extractor

import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.model.provider.MediaLink
import com.flixclusive.provider.extractor.Extractor
import com.flxProviders.sudoflix.api.primewire.util.ExtractorHelper.getRedirectedUrl
import com.flxProviders.sudoflix.api.primewire.util.ExtractorHelper.unpackLinks
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

internal class MixDrop(
    client: OkHttpClient
) : Extractor(client) {
    override val baseUrl = "https://mixdrop.ag"
    override val name = "MixDrop"

    override suspend fun extract(
        url: String,
        customHeaders: Map<String, String>?
    ): List<MediaLink> {
        val cleanedUrl = getRedirectedUrl(
            client = client,
            url = url,
            domainName = "mixdrop"
        ).toHttpUrl()
        val probableHost = cleanedUrl.host
        val embedId = cleanedUrl.pathSegments
            .lastOrNull()
            ?: throw Exception("[$name]> Failed to find embed id")

        return unpackLinks(
            client = client,
            url = "https://$probableHost/e/$embedId",
            headers = Headers.headersOf(
                "User-Agent", "PostmanRuntime/7.41.2"
            ),
            linkRegex = Regex("""MDCore\.wurl="(.*?)";"""),
        ).mapAsync { stream ->
            when {
                stream.url.startsWith("http") -> stream
                else -> stream.copy(url = "https:${stream.url}")
            }
        }
    }
}