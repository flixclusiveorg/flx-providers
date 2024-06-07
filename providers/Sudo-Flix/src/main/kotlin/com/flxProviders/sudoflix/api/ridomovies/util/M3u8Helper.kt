package com.flxProviders.sudoflix.api.ridomovies.util

import com.flixclusive.core.util.coroutines.mapIndexedAsync
import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import okhttp3.OkHttpClient

internal suspend fun extractQualitiesAndLinks(
    client: OkHttpClient,
    serverName: String,
    source: String,
    onLinkLoaded: (SourceLink) -> Unit
) {
    client.request(url = source)
        .execute().body
        ?.string()
        ?.let { data ->
            val urls = data
                .split('\n')
                .filter { line -> line.contains(".m3u8") }

            val qualities = data
                .split('\n')
                .filter { line -> line.contains("RESOLUTION=") }

            qualities.mapIndexedAsync { i, s ->
                val qualityTag = "$serverName: ${s.split('x')[1]}p"
                val dataUrl = urls[i]

                onLinkLoaded(
                    SourceLink(
                        name = qualityTag,
                        url = dataUrl
                    )
                )
            }
        }
}