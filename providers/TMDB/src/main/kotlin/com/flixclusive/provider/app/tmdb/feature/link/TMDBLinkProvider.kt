package com.flixclusive.provider.app.tmdb.feature.link

import androidx.compose.ui.util.fastForEach
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.core.util.network.jsoup.asJsoup
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.provider.app.tmdb.core.config.DEFAULT_LANGUAGE
import com.flixclusive.provider.app.tmdb.core.config.KEY_LANGUAGE
import com.flixclusive.provider.capability.MediaLinkProviderApi
import com.flixclusive.provider.capability.MediaLinkType
import com.flixclusive.provider.extensions.getString
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import java.net.URLDecoder

internal class TMDBLinkProvider(
    private val client: OkHttpClient,
    private val settings: DataStore<Preferences>,
) : MediaLinkProviderApi {

    override val supportedLinkTypes: Set<MediaLinkType> = setOf(MediaLinkType.STREAMS)

    override suspend fun getLinks(
        media: MediaMetadata,
        episode: Episode?,
        onLinkFound: (MediaLink) -> Unit,
    ) {
        val language = settings.getString(KEY_LANGUAGE, null) ?: DEFAULT_LANGUAGE
        val locale = language.substringAfter('-', "US").uppercase().take(2).ifBlank { "US" }
        val tmdbId = media.externalIds[MediaIdSource.TMDB] ?: media.id
        val mediaTypePath = if (media.type.isMovie) "movie" else "tv"
        val url = "https://www.themoviedb.org/$mediaTypePath/$tmdbId/watch?locale=$locale"
        val html = FlxDispatchers.withIOContext {
            client.request(url = url).execute().asJsoup()
        }
        parseStreamingInfo(html).forEach { onLinkFound(it) }
    }

    private fun parseStreamingInfo(html: Document): List<Stream> {
        val streamingInfoList = mutableListOf<Stream>()
        html.select("div.ott_provider li a").fastForEach { element ->
            val href = element.attr("href")
            val title = element.attr("title")
            val logoUrl = element.select("img").attr("src")
            val providerName = title.split(" on ").lastOrNull()?.trim() ?: "Unknown Provider"
            val url = href
                .split("&r=")
                .getOrNull(1)
                ?.split("&")
                ?.firstOrNull()
                ?.let { URLDecoder.decode(it, "UTF-8") }
            if (url != null && streamingInfoList.none { it.url == url }) {
                streamingInfoList.add(
                    Stream(
                        name = providerName,
                        description = title,
                        url = url,
                        flags = setOf(
                            Flag.ThirdPartyGateway(
                                name = providerName,
                                logo = logoUrl.takeIf { it.isNotBlank() },
                            ),
                        ),
                    ),
                )
            }
        }
        return streamingInfoList
    }
}
