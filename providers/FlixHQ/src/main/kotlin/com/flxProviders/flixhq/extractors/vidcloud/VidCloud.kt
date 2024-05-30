package com.flxProviders.flixhq.extractors.vidcloud

import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.coroutines.mapIndexedAsync
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.CryptographyUtil.decryptAes
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.provider.extractor.Extractor
import com.flxProviders.flixhq.extractors.vidcloud.dto.DecryptedSource
import com.flxProviders.flixhq.extractors.vidcloud.dto.VidCloudEmbedData
import com.flxProviders.flixhq.extractors.vidcloud.dto.VidCloudEmbedData.Companion.toSubtitle
import com.flxProviders.flixhq.extractors.vidcloud.dto.VidCloudEmbedDataCustomDeserializer
import com.flxProviders.flixhq.extractors.vidcloud.dto.VidCloudKey
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.net.URL

/**
 *
 * Also known as vidcloud
 * */
class VidCloud(
    private val client: OkHttpClient,
) : Extractor() {
    override val name: String = "upcloud"
    override val alternateNames: List<String>
        get() = listOf("vidcloud")
    override val host: String = "https://rabbitstream.net"

    var key = VidCloudKey()

    override suspend fun extract(
        url: URL,
        mediaId: String,
        episodeId: String,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ) {
        if (key.e4Key.isEmpty() || key.kId.isEmpty() || key.kVersion.isEmpty() || key.browserVersion.isEmpty()) {
            throw Exception("FlixHQ key not set. Go to FlixHQ settings and get the keys!")
        }

        val id = url.path.split('/').last().split('?').first()
        val options = Headers.Builder()
            .add("X-Requested-With", "XMLHttpRequest")
            .add("Referer", url.toString())
            .build()

        val sourceEndpoint = "$host/ajax/v2/embed-4/getSources?id=$id&v=${key.kVersion}&h=${key.kId}&b=${key.browserVersion}"
        val response = client.request(
            url = sourceEndpoint,
            headers = options,
        ).execute()

        val responseBody = response.body
            ?.string()
            ?: throw Exception("Cannot fetch source")

        if(responseBody.isBlank())
            throw Exception("Cannot fetch source")

        val vidCloudEmbedData = fromJson<VidCloudEmbedData>(
            json = responseBody,
            serializer = VidCloudEmbedDataCustomDeserializer {
                safeCall {
                    fromJson<List<DecryptedSource>>(decryptAes(it, key.e4Key))
                } ?: throw IllegalStateException("Key might be outdated!")
            }
        )

        vidCloudEmbedData.run {
            if (sources.isEmpty()) {
                return@run
            }

            onLinkLoaded(
                SourceLink(
                    url = sources[0].url,
                    name = "$name: Auto"
                )
            )

            asyncCalls(
                {
                    sources.mapAsync { source ->
                        client.request(
                            url = source.url,
                            headers = options
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

                                    onLinkLoaded(
                                        SourceLink(
                                            name = qualityTag,
                                            url = dataUrl
                                        )
                                    )
                                }
                            }
                    }
                },
                {
                    vidCloudEmbedData.tracks.mapAsync {
                        onSubtitleLoaded(it.toSubtitle())
                    }
                }
            )
        }
    }
}