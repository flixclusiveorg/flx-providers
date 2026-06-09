package com.flixclusive.provider.app.stremio.feature.metadata

import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.provider.app.stremio.StremioPlugin
import com.flixclusive.provider.app.stremio.core.model.FetchMetaResponse
import com.flixclusive.provider.app.stremio.core.model.toMedia
import com.flixclusive.provider.app.stremio.core.util.AddonUtil
import com.flixclusive.provider.app.stremio.core.util.AddonUtil.ADDON_SOURCE_KEY
import com.flixclusive.provider.app.stremio.core.util.AddonUtil.MEDIA_TYPE_KEY
import com.flixclusive.provider.app.stremio.core.util.AddonUtil.getAddon
import com.flixclusive.provider.capability.MediaMetadataProviderApi
import okhttp3.OkHttpClient

class StremioMetadataProvider internal constructor(
    private val client: OkHttpClient,
    private val plugin: StremioPlugin
): MediaMetadataProviderApi {
    override suspend fun getMovie(media: PartialMedia): Movie {
        return getMetadataFromAddon(media) as Movie
    }

    override suspend fun getShow(media: PartialMedia): Show {
        return getMetadataFromAddon(media) as Show
    }

    private suspend fun getMetadataFromAddon(
        media: PartialMedia
    ): MediaMetadata {
        val nameKey = "${media.id}=${media.title}"
        val actualId = media.externalIds[MediaIdSource.IMDB] ?: media.id
        val hasImdbId = media.externalIds[MediaIdSource.IMDB] != null || media.id.startsWith("tt")

        val addonSource = media.customProperties[ADDON_SOURCE_KEY]
            ?: throw IllegalArgumentException("[$nameKey]> Addon source must not be null!")
        val type = media.customProperties[MEDIA_TYPE_KEY]
            ?: throw IllegalArgumentException("[$nameKey]> Media type must not be null!")

        if (addonSource == AddonUtil.DEFAULT_META_PROVIDER_ID) {
            return getFilmDetailsFromDefaultMetaProvider(
                id = actualId,
                type = type,
                nameKey = nameKey
            )
        }

        val addon = plugin.settings.getAddon(id = addonSource)
        require(addon.hasMeta || hasImdbId) {
            "[$nameKey]> Addon has no metadata resource!"
        }

        if (!addon.hasMeta && hasImdbId) {
            return getFilmDetailsFromDefaultMetaProvider(
                id = actualId,
                type = type,
                nameKey = nameKey
            )
        }

        val url = "${addon.baseUrl}/meta/$type/${media.id}.json"
        return getMetadata(
            addonId = addonSource,
            nameKey = nameKey,
            url = url
        )
    }

    private suspend fun getMetadata(
        addonId: String,
        nameKey: String,
        url: String
    ): MediaMetadata {
        val failedToFetchMetaDataMessage = "[${addonId}]> Coudn't fetch meta data of $nameKey ($url)"
        val response = FlxDispatchers.withIOContext {
            client.request(url = url).execute()
                .fromJson<FetchMetaResponse>(errorMessage = failedToFetchMetaDataMessage)
        }

        if (response.err != null) {
            throw Exception(failedToFetchMetaDataMessage)
        }

        return response.media?.toMedia(
            addonId = addonId,
            providerId = plugin.id
        ) ?: throw IllegalArgumentException("[$nameKey]> Meta data is null!")
    }

    private suspend fun getFilmDetailsFromDefaultMetaProvider(
        id: String,
        type: String,
        nameKey: String
    ): MediaMetadata {
        val url = "${AddonUtil.DEFAULT_META_PROVIDER_BASE_URL}/meta/$type/$id.json"

        return getMetadata(
            addonId = AddonUtil.DEFAULT_META_PROVIDER_ID,
            nameKey = nameKey,
            url = url
        )
    }

    override suspend fun getSeason(
        show: Show,
        season: Season.Partial
    ): Season.Full? {
        return null
    }
}