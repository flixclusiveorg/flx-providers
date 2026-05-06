package com.flixclusive.provider.app.trakt.feature.crossmatch

import android.content.Context
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.provider.capability.CrossMatchProviderApi
import com.flixclusive.provider.app.trakt.TraktPlugin
import com.flixclusive.provider.app.trakt.core.config.TraktApiConfig.SEARCH_V2_URL
import com.flixclusive.provider.app.trakt.core.config.TypeSenseKeyProvider
import com.flixclusive.provider.app.trakt.core.model.TraktMedia.Companion.toMovie
import com.flixclusive.provider.app.trakt.core.model.TraktMedia.Companion.toShow
import com.flixclusive.provider.app.trakt.core.network.TraktApiService
import com.flixclusive.provider.app.trakt.core.network.TraktApiService.Companion.getFullSeasons
import com.flixclusive.provider.app.trakt.core.network.dto.request.TraktSearchRequestV2
import com.flixclusive.provider.app.trakt.core.network.util.OkHttpClientUtil
import java.util.Calendar

class TraktCrossMatcher internal constructor(
    private val context: Context,
    private val plugin: TraktPlugin,
    private val typeSenseKeyProvider: TypeSenseKeyProvider,
) : CrossMatchProviderApi {
    companion object {
        private const val THRESHOLD_SCORE = 0.75
    }

    private val defaultApiService by lazy {
        TraktApiService.create(
            OkHttpClientUtil.createCachedClient(
                context = context,
                settings = plugin.settings,
                cacheMaxAge = 60 * 60 // 1 hour in seconds
            )
        )
    }

    override suspend fun getByFuzzy(media: MediaMetadata): MediaMetadata? {
        val title = media.title
        val year = media.releaseDate?.let {
            // check if the release date is in milliseconds or in seconds, if it's in seconds, convert it to milliseconds
            val time = if (it < 1000000000000) it * 1000 else it

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = time
            calendar.get(Calendar.YEAR)
        }

        val requestType = if (media.isMovie) "Movie" else "Show"
        val query = if (media.isMovie) "$title $year" else title

        if (typeSenseKeyProvider.typeSenseKey == null) {
            typeSenseKeyProvider.reloadTypeSenseKey()
        }

        val page = 0
        val limit = 100
        val url = "$SEARCH_V2_URL?q=$query&limit=$limit&page=$page"
        val result = defaultApiService.search(
            apiKey = typeSenseKeyProvider.typeSenseKey!!,
            url = url,
            requestBody = TraktSearchRequestV2(
                searches = listOf(
                    TraktSearchRequestV2.SearchRequest(
                        collection = requestType,
                    )
                )
            )
        )

        val bestMatch = result.getBestMatch()?.toTraktMedia()

        return if (media.isMovie) {
            bestMatch?.toMovie(plugin.id)
        } else {
            bestMatch?.toShow(
                providerId = plugin.id,
                seasons = defaultApiService.getFullSeasons(bestMatch.id)
            )
        }
    }

    override suspend fun getById(sourceIds: Map<MediaIdSource, String>): MediaMetadata? {
        val ids = sourceIds.map { (source, id) ->
            source.name.lowercase() to id
        }

        val (idType, id) = ids.firstOrNull() ?: return null

        val medias = defaultApiService.searchById(
            id = id,
            idType = idType,
        )

        val metadata = if (medias.size > 1) {
            val scoredMedias = medias.filter { media ->
                media.score >= THRESHOLD_SCORE
            }.sortedByDescending { it.score }

            scoredMedias.firstOrNull()
        } else {
            medias.firstOrNull()
        }

        if (metadata == null) return null

        return when (metadata.isMovie) {
            true -> metadata.media.toMovie(plugin.id)
            false -> metadata.media.toShow(
                providerId = plugin.id,
                seasons = defaultApiService.getFullSeasons(metadata.id)
            )
        }
    }
}