package com.flxProviders.flixhq.extractors.vidcloud

import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.coroutines.mapIndexedAsync
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
    private val key: VidCloudKey,
    private val client: OkHttpClient,
    private val isAlternative: Boolean = false,
) : Extractor() {
    override val name: String = "vidcloud"
    override val alternateNames: List<String>
        get() = listOf("upcloud")

    override val host: String = "https://rabbitstream.net"
    private val alternateHost: String = "https://dokicloud.one"

    private fun getHost(isAlternative: Boolean) =
        (if (isAlternative) "DokiCloud" else "Rabbitstream")

    override suspend fun extract(
        url: URL,
        mediaId: String,
        episodeId: String,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ) {
        val id = url.path.split('/').last().split('?').first()
        val options = Headers.Builder()
            .add("X-Requested-With", "XMLHttpRequest")
            .add("Referer", url.toString())
            .build()

        val hostToUse = if (isAlternative) alternateHost else host

        val sourceEndpoint = "$hostToUse/ajax/v2/embed-4/getSources?id=$id&v=${key.kVersion}&h=${key.kId}&b=${key.browserVersion}"
        val response = client.request(
            url = sourceEndpoint,
            headers = options
        ).execute()

        val responseBody = response.body
            ?.string()
            ?: throw Exception("Cannot fetch source")

        if(responseBody.isBlank())
            throw Exception("Cannot fetch source")

        val vidCloudEmbedData = fromJson<VidCloudEmbedData>(
            json = responseBody,
            serializer = VidCloudEmbedDataCustomDeserializer {
                fromJson<List<DecryptedSource>>(
                    decryptAes(it, key.e4Key)
                )
            }
        )

        vidCloudEmbedData.run {
            check(sources.isNotEmpty())
            onLinkLoaded(
                SourceLink(
                    url = sources[0].url,
                    name = "${getHost(isAlternative)}: " + "Auto"
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
                                    val qualityTag = "${getHost(isAlternative)}: ${s.split('x')[1]}p"
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