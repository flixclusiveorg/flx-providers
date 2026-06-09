package com.flixclusive.provider.app.tmdb.feature.metadata

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.provider.app.tmdb.core.config.APPEND_TO_RESPONSE
import com.flixclusive.provider.app.tmdb.core.config.TMDB_API_BASE_URL
import com.flixclusive.provider.app.tmdb.core.config.readImageConfig
import com.flixclusive.provider.app.tmdb.core.model.dto.MovieDetailDto
import com.flixclusive.provider.app.tmdb.core.model.dto.SeasonDto
import com.flixclusive.provider.app.tmdb.core.model.dto.TvShowDetailDto
import com.flixclusive.provider.capability.MediaMetadataProviderApi
import okhttp3.OkHttpClient

internal class TMDBMetadataProvider(
    private val client: OkHttpClient,
    private val settings: DataStore<Preferences>,
    private val providerId: String,
) : MediaMetadataProviderApi {

    override suspend fun getMovie(media: PartialMedia): Movie {
        val imgCfg = settings.readImageConfig()
        val tmdbId = media.externalIds[MediaIdSource.TMDB] ?: media.id
        val url = "${TMDB_API_BASE_URL}movie/$tmdbId?append_to_response=$APPEND_TO_RESPONSE"
        val dto = FlxDispatchers.withIOContext {
            client.request(url = url).execute().fromJson<MovieDetailDto>()
        }
        return dto.toMovie(providerId, imgCfg)
    }

    override suspend fun getShow(media: PartialMedia): Show {
        val imgCfg = settings.readImageConfig()
        val tmdbId = media.externalIds[MediaIdSource.TMDB] ?: media.id
        val url = "${TMDB_API_BASE_URL}tv/$tmdbId?append_to_response=$APPEND_TO_RESPONSE"
        val showDto = FlxDispatchers.withIOContext {
            client.request(url = url).execute().fromJson<TvShowDetailDto>()
        }

        return showDto.toShow(providerId, imgCfg)
    }

    override suspend fun getSeason(
        show: Show,
        season: Season.Partial
    ): Season.Full? {
        val tmdbId = show.externalIds[MediaIdSource.TMDB] ?: show.id
        val seasonUrl = "${TMDB_API_BASE_URL}tv/$tmdbId/season/${season.number}"
        return safeCall {
            FlxDispatchers.withIOContext {
                client.request(url = seasonUrl).execute()
                    .fromJson<SeasonDto>()
                    .toSeason()
            }
        }
    }
}
