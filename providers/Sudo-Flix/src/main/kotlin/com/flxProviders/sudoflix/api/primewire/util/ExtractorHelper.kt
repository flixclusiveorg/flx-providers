package com.flxProviders.sudoflix.api.primewire.util

import com.flixclusive.core.util.network.request
import com.flixclusive.provider.extractor.Extractor
import okhttp3.OkHttpClient
import java.net.URL

internal fun Extractor.getRedirectedUrl(
    client: OkHttpClient,
    url: URL,
): String {
    return client.request(url = url.toString())
        .execute()
        .use {
            val redirectedUrl = it.request.url
            if (!it.isSuccessful && !redirectedUrl.host.contains("upstream")) {
                throw Exception("[$name]> Failed to get redirect URL")
            }

            redirectedUrl.toString()
        }
}