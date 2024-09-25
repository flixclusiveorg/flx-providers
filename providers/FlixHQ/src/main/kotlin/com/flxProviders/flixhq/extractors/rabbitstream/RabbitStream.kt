package com.flxProviders.flixhq.extractors.rabbitstream

import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.coroutines.mapIndexedAsync
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.Crypto.decryptAes
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.provider.extractor.EmbedExtractor
import com.flxProviders.flixhq.extractors.rabbitstream.dto.DecryptedSource
import com.flxProviders.flixhq.extractors.rabbitstream.dto.VidCloudEmbedData
import com.flxProviders.flixhq.extractors.rabbitstream.dto.VidCloudEmbedData.Companion.toSubtitle
import com.flxProviders.flixhq.extractors.rabbitstream.dto.VidCloudEmbedDataCustomDeserializer
import com.flxProviders.flixhq.extractors.rabbitstream.dto.VidCloudKey
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.net.URL

internal class VidCloud(client: OkHttpClient): RabbitStream(client) {
    override val name = "VidCloud"
}
internal class UpCloud(client: OkHttpClient): RabbitStream(client) {
    override val name = "UpCloud"
}


internal open class RabbitStream(
    client: OkHttpClient,
) : EmbedExtractor(client) {
    override val name: String = "RabbitStream"
    override val baseUrl: String = "https://rabbitstream.net"

    suspend fun extract(
        url: String,
        key: VidCloudKey,
        userAgent: String,
        onLinkFound: (MediaLink) -> Unit
    ) {
        if (key.e4Key.isEmpty() || key.kId.isEmpty() || key.kVersion.isEmpty() || key.browserVersion.isEmpty()) {
            throw Exception("Key has not been set!")
        }

        val id = URL(url).path
            .split('/').last()
            .split('?').first()
        val options = Headers.Builder()
            .add("X-Requested-With", "XMLHttpRequest")
            .add("Referer", url)
            .build()

        val sourceEndpoint = "$baseUrl/ajax/v2/embed-4/getSources?id=$id&v=${key.kVersion}&h=${key.kId}&b=${key.browserVersion}"
        val response = client.request(
            url = sourceEndpoint,
            userAgent = userAgent,
            headers = options
        ).execute()

        val responseBody = response.body?.string()
            ?: throw Exception("Cannot fetch source")

        if(responseBody.isBlank())
            throw Exception("Cannot fetch source")

        val data = fromJson<VidCloudEmbedData>(
            json = responseBody,
            serializer = VidCloudEmbedDataCustomDeserializer {
                safeCall {
                    fromJson<List<DecryptedSource>>(decryptAes(it, key.e4Key))
                } ?: throw IllegalStateException("Key might be outdated!")
            }
        )

        if (data.sources.isEmpty()) {
            throw Exception("Sources are empty!")
        }

        onLinkFound(
            Stream(
                url = data.sources[0].url,
                name = "$name: Auto"
            )
        )

        asyncCalls(
            {
                data.sources.mapAsync { source ->
                    client.request(
                        url = source.url,
                        headers = options,
                    ).execute().body
                        ?.string()
                        ?.let { data ->
                            val urls = data
                                .split('\n')
                                .filter { line -> line.contains(".m3u8") }

                            val qualities = data
                                .split('\n')
                                .filter { line -> line.contains("RESOLUTION=") }

                            qualities.mapIndexedAsync { i, s ->
                                val qualityTag = "$name: ${s.split('x')[1]}p"
                                val dataUrl = urls[i]

                                onLinkFound(
                                    Stream(
                                        name = qualityTag,
                                        url = dataUrl
                                    )
                                )
                            }
                        }
                }
            },
            {
                data.tracks.mapAsync {
                    onLinkFound(it.toSubtitle())
                }
            }
        )
    }

    override suspend fun extract(
        url: String,
        customHeaders: Map<String, String>?,
        onLinkFound: (MediaLink) -> Unit
    ) {
        throw IllegalStateException("Called the wrong extract method for this extractor")
    }
}