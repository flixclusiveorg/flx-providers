package com.flxProviders.sudoflix.api.primewire.extractor

import com.flixclusive.model.provider.MediaLink
import com.flixclusive.provider.extractor.Extractor
import com.flxProviders.sudoflix.api.primewire.util.ExtractorHelper.unpackLinks
import okhttp3.OkHttpClient

internal class FileLions(
    client: OkHttpClient
) : Extractor(client) {
    override val baseUrl = "https://filelions.site"
    override val name = "FileLions"

    override suspend fun extract(
        url: String,
        customHeaders: Map<String, String>?
    ): List<MediaLink> {
        return unpackLinks(
            client = client,
            url = url
        )
    }
}