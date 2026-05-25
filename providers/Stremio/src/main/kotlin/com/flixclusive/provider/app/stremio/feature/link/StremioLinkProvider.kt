package com.flixclusive.provider.app.stremio.feature.link

import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.provider.app.stremio.StremioPlugin
import com.flixclusive.provider.app.stremio.core.model.Addon
import com.flixclusive.provider.app.stremio.core.model.EMBEDDED_IMDB_ID_KEY
import com.flixclusive.provider.app.stremio.core.model.EMBEDDED_STREAM_KEY
import com.flixclusive.provider.app.stremio.core.model.StreamResponse
import com.flixclusive.provider.app.stremio.core.model.SubtitleResponse
import com.flixclusive.provider.app.stremio.core.util.AddonUtil
import com.flixclusive.provider.app.stremio.core.util.AddonUtil.ADDON_SOURCE_KEY
import com.flixclusive.provider.app.stremio.core.util.AddonUtil.MEDIA_TYPE_KEY
import com.flixclusive.provider.app.stremio.core.util.AddonUtil.getAddon
import com.flixclusive.provider.app.stremio.core.util.AddonUtil.getAddons
import com.flixclusive.provider.app.stremio.core.util.isValidUrl
import com.flixclusive.provider.capability.MediaLinkProviderApi
import com.flixclusive.provider.capability.MediaLinkType
import okhttp3.OkHttpClient

class StremioLinkProvider internal constructor(
    private val client: OkHttpClient,
    private val plugin: StremioPlugin,
) : MediaLinkProviderApi {
    override val supportedLinkTypes = setOf(
        MediaLinkType.STREAMS,
        MediaLinkType.SUBTITLES
    )

    override suspend fun getLinks(
        media: MediaMetadata,
        episode: Episode?,
        onLinkFound: (MediaLink) -> Unit,
    ) {
        media.fetchLinks(
            episode = episode,
            onLinkFound = onLinkFound
        )
    }

    private suspend fun MediaMetadata.fetchLinks(
        episode: Episode?,
        onLinkFound: (MediaLink) -> Unit
    ) {
        val embeddedStream = customProperties[EMBEDDED_STREAM_KEY]
        if (embeddedStream != null) {
            val stream = Stream(
                name = title,
                url = embeddedStream
            )

            return onLinkFound(stream)
        }

        val addons = plugin.settings.getAddons()
        val streamType = customProperties[MEDIA_TYPE_KEY]
        val addonId = customProperties[ADDON_SOURCE_KEY]
        val isFromStremio = providerId == plugin.id
        val id = if (isFromStremio) id else customProperties[EMBEDDED_IMDB_ID_KEY]
            ?: throw IllegalArgumentException("[$id]> IMDB ID should not be null!")

        if (addonId != null && addonId != AddonUtil.DEFAULT_META_PROVIDER_ID) {
            val addonSource = plugin.settings.getAddon(id = addonId)
            if (addonSource.hasStream) {
                addonSource.getStream(
                    id = id,
                    mediaType = type,
                    isFromStremio = isFromStremio,
                    streamType = streamType,
                    episode = episode,
                    addonName = addonSource.name,
                    onLinkFound = onLinkFound,
                )
            }
        }

        val mediaIdForSubtitles = customProperties[EMBEDDED_IMDB_ID_KEY]
            ?: externalIds[MediaIdSource.IMDB]
            ?: id

        addons.forEach { addon ->
            if (addon.hasStream) {
                addon.getStream(
                    id = id,
                    mediaType = type,
                    streamType = streamType,
                    episode = episode,
                    isFromStremio = isFromStremio,
                    addonName = addon.name,
                    onLinkFound = onLinkFound
                )
            }

            if (addon.hasSubtitle) {
                addon.getSubtitle(
                    id = mediaIdForSubtitles,
                    episode = episode,
                    onLinkFound = onLinkFound
                )
            }
        }
    }

    private suspend fun Addon.getSubtitle(
        id: String,
        episode: Episode?,
        onLinkFound: (MediaLink) -> Unit
    ) {
        val slug = if(episode == null) {
            "subtitles/movie/$id"
        } else {
            "subtitles/series/$id:${episode.season}:${episode.number}"
        }

        val response = FlxDispatchers.withIOContext {
            client.request(url = "$baseUrl/$slug.json").execute()
        }.fromJson<SubtitleResponse>()

        if (response.err != null)
            return

        response.subtitles.forEach { subtitle ->
            val isValidUrl = isValidUrl(subtitle.url)
            if (!isValidUrl) return@forEach

            onLinkFound(
                subtitle.toSubtitle(addonName = this@getSubtitle.name)
            )
        }
    }

    private suspend fun Addon.getStream(
        id: String,
        mediaType: MediaType,
        isFromStremio: Boolean,
        streamType: String?,
        episode: Episode?,
        addonName: String,
        onLinkFound: (MediaLink) -> Unit
    ) {
        val hasStreamUrlOnEpisode = episode != null && isValidUrl(episode.id)
        if (hasStreamUrlOnEpisode) {
            val stream = Stream(
                name = episode.title ?: "S${episode.season}E${episode.number}",
                url = episode.id
            )

            return onLinkFound(stream)
        }

        val query = when (streamType) {
            null -> getStreamQuery(
                id = id,
                type = mediaType,
                isFromStremio = isFromStremio,
                episode = episode
            )
            else -> getStreamQuery(
                id = id,
                type = streamType,
                isFromStremio = isFromStremio,
                episode = episode
            )
        }

        val response = FlxDispatchers.withIOContext {
            client.request(
                url = "$baseUrl/$query"
            ).execute()
        }.fromJson<StreamResponse>()

        if (response.err != null)
            return

        response.streams.forEach { stream ->
            val streamLink = stream.toStreamLink()
            if (streamLink != null)
                onLinkFound(streamLink)

            stream.subtitles?.forEach sub@ { subtitle ->
                val isValidUrl = isValidUrl(subtitle.url)
                if (!isValidUrl) return@sub

                onLinkFound(subtitle.toSubtitle(addonName))
            }
        }
    }
}