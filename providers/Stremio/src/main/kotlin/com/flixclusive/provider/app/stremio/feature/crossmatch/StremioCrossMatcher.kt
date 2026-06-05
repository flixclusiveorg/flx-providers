package com.flixclusive.provider.app.stremio.feature.crossmatch

import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.provider.app.stremio.StremioPlugin
import com.flixclusive.provider.app.stremio.core.model.FetchMetaResponse
import com.flixclusive.provider.app.stremio.core.model.toMedia
import com.flixclusive.provider.app.stremio.core.util.AddonUtil
import com.flixclusive.provider.app.stremio.feature.search.StremioSearchProvider
import com.flixclusive.provider.capability.CrossMatchProviderApi
import okhttp3.OkHttpClient
import java.util.Calendar
import java.util.Date

class StremioCrossMatcher internal constructor(
    private val client: OkHttpClient,
    private val searchProvider: StremioSearchProvider,
    private val plugin: StremioPlugin
) : CrossMatchProviderApi {
    override suspend fun getByFuzzy(media: MediaMetadata): MediaMetadata? {
        val response = searchProvider.search(media.title)

        val bestMatch = response.results.firstOrNull { result ->
            // Check if one of media.externalIds is inside result.externalIds, if media has externalIds
            val hasMatchingExternalId = media.externalIds.any { (source, id) ->
                val resultExternalId = result.externalIds[source]
                resultExternalId != null && resultExternalId == id
            }

            if (hasMatchingExternalId) {
                return@firstOrNull true
            }

            val titleMatches = result.title.matchesFuzzy(media.title)
            val yearMatches = result.releaseDate?.yearMatches(media.releaseDate) == true

            titleMatches && yearMatches
        } ?: return null

        return getMetadata(
            imdbId = bestMatch.externalIds[MediaIdSource.IMDB] ?: return null,
            type = when (bestMatch.type) {
                MediaType.MOVIE -> "movie"
                MediaType.SHOW -> "series"
            }
        )
    }

    override suspend fun getById(sourceIds: Map<MediaIdSource, String>): MediaMetadata? {
        val imdbId = sourceIds[MediaIdSource.IMDB] ?: return null

        return safeCall {
            getMetadata(
                imdbId = imdbId,
                type = "movie"
            )
        } ?: safeCall {
            getMetadata(
                imdbId = imdbId,
                type = "series"
            )
        }
    }

    @Throws
    private suspend fun getMetadata(
        imdbId: String,
        type: String
    ): MediaMetadata? {
        val cinemataId = AddonUtil.DEFAULT_META_PROVIDER_ID
        val cinemataUrl = "${AddonUtil.DEFAULT_META_PROVIDER_BASE_URL}/meta/$type/$imdbId.json"

        val response = FlxDispatchers.withIOContext {
            safeCall {
                client.request(url = cinemataUrl).execute()
                    .fromJson<FetchMetaResponse>()
            }
        }

        if (response == null || response.err != null) {
            error("Failed to fetch metadata from default meta provider: ${response?.err}")
        }

        return response.media?.toMedia(
            addonId = cinemataId,
            providerId = plugin.id
        )
    }

    private fun Long.yearMatches(other: Long?): Boolean {
        if (other == null) return true

        // Compare thru Calendar
        val calendar1 = Calendar.getInstance().apply { time = Date(this@yearMatches) }
        val calendar2 = Calendar.getInstance().apply { time = Date(other) }

        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
    }

    private fun String.matchesFuzzy(other: String?): Boolean {
        if (other == null) return true

        val normalizedThis = this.lowercase()
        val normalizedOther = other.lowercase()

        return normalizedThis == normalizedOther
    }
}