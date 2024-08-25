package com.flxProviders.sudoflix.api.primewire.extractor

import com.flixclusive.model.provider.MediaLink
import com.flixclusive.provider.extractor.Extractor
import com.flxProviders.sudoflix.api.primewire.util.ExtractorHelper.getRedirectedUrl
import com.flxProviders.sudoflix.api.primewire.util.ExtractorHelper.unpackLinks
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

internal class VTube(
    client: OkHttpClient
) : Extractor(client) {
    override val baseUrl = "https://vtube.network"
    override val name = "VTube"

    override suspend fun extract(
        url: String,
        customHeaders: Map<String, String>?
    ): List<MediaLink> {
        val newUrl = baseUrl.toHttpUrl()
        val safeUrl = getRedirectedUrl(
            client = client,
            url = url,
            domainName = "vtube"
        ).toHttpUrl()
            .newBuilder()
            .scheme(newUrl.scheme)
            .host(newUrl.host)
            .build()
            .toString()

        return unpackLinks(
            client = client,
            url = safeUrl
        )
    }
}