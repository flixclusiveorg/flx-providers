package com.flixclusive.provider.app.trakt.feature.metadata

import android.content.Context
import androidx.compose.ui.util.fastMap
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.provider.app.trakt.TraktPlugin
import com.flixclusive.provider.app.trakt.core.model.TraktMedia.Companion.toMovie
import com.flixclusive.provider.app.trakt.core.model.TraktMedia.Companion.toPartialMedia
import com.flixclusive.provider.app.trakt.core.model.TraktMedia.Companion.toShow
import com.flixclusive.provider.app.trakt.core.network.TraktApiService
import com.flixclusive.provider.app.trakt.core.network.dto.response.TraktEpisodeResponse.Companion.toEpisode
import com.flixclusive.provider.app.trakt.core.network.dto.response.TraktSeasonResponse.Companion.toSeason
import com.flixclusive.provider.app.trakt.core.network.util.OkHttpClientUtil
import com.flixclusive.provider.capability.MediaMetadataProviderApi

class TraktMetadata internal constructor(
    private val context: Context,
    private val plugin: TraktPlugin,
) : MediaMetadataProviderApi {

    private val cachedApiService = TraktApiService.create(
        OkHttpClientUtil.createCachedClient(
            context = context,
            settings = plugin.settings,
            cacheMaxAge = 60 * 60 * 24 * 4 // 4 days in seconds
        )
    )

    override suspend fun getMovie(media: PartialMedia): Movie {
        val related = safeCall {
            cachedApiService.getRelatedItems(
                type = "movies",
                id = media.id,
                limit = 30
            )
        } ?: emptyList()

        return cachedApiService.getMetadata(
            type = "movies",
            id = media.id,
        ).toMovie(plugin.id).copy(
            recommendations = related.fastMap {
                it.toPartialMedia(plugin.id)
            }
        )
    }

    override suspend fun getShow(media: PartialMedia): Show {
        val seasons = cachedApiService.getSeasons(showId = media.id)
        val related = safeCall {
            cachedApiService.getRelatedItems(
                type = "shows",
                id = media.id,
                limit = 30
            )
        } ?: emptyList()

        return cachedApiService.getMetadata(
            type = "shows",
            id = media.id
        ).toShow(
            providerId = plugin.id,
            seasons = seasons.fastMap { it.toSeason() }
        ).copy(
            recommendations = related.fastMap {
                it.toPartialMedia(plugin.id)
            }
        )
    }

    override suspend fun getSeason(
        show: Show,
        season: Season.Partial
    ): Season.Full {
        val traktEpisodes = cachedApiService.getEpisodes(show.id, season.number)
        val episodes = traktEpisodes.fastMap { episode -> episode.toEpisode() }

        return Season.Full(
            id = season.id,
            number = season.number,
            isReleased = season.isReleased,
            title = season.title,
            releaseDate = season.releaseDate,
            overview = season.overview,
            rating = season.rating,
            image = season.image,
            episodes = episodes
        )
    }
}