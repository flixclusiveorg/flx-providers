package com.flixclusive.provider.app.tmdb.feature.crossmatch

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.provider.app.tmdb.core.config.APPEND_TO_RESPONSE
import com.flixclusive.provider.app.tmdb.core.config.TMDB_API_BASE_URL
import com.flixclusive.provider.app.tmdb.core.config.readImageConfig
import com.flixclusive.provider.app.tmdb.core.model.dto.FilmSearchItemDto
import com.flixclusive.provider.app.tmdb.core.model.dto.MovieDetailDto
import com.flixclusive.provider.app.tmdb.core.model.dto.SearchPageDto
import com.flixclusive.provider.app.tmdb.core.model.dto.SeasonDto
import com.flixclusive.provider.app.tmdb.core.model.dto.TvShowDetailDto
import com.flixclusive.provider.capability.CrossMatchProviderApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import java.net.URLEncoder

internal class TMDBCrossMatcher(
    private val client: OkHttpClient,
    private val settings: DataStore<Preferences>,
    private val providerId: String,
) : CrossMatchProviderApi {

    override suspend fun getById(sourceIds: Map<MediaIdSource, String>): MediaMetadata? {
        val tmdbId = sourceIds[MediaIdSource.TMDB] ?: return null
        return try {
            fetchMovie(tmdbId)
        } catch (_: Throwable) {
            try {
                fetchShow(tmdbId)
            } catch (_: Throwable) {
                null
            }
        }
    }

    override suspend fun getByFuzzy(media: MediaMetadata): MediaMetadata? {
        val title = media.title.takeIf { it.isNotBlank() } ?: return null

        val endpoint = when (media.type) {
            MediaType.MOVIE -> "search/movie"
            MediaType.SHOW -> "search/tv"
        }
        val hint = if (media.type.isMovie) "movie" else "tv"
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        val url = "${TMDB_API_BASE_URL}${endpoint}?query=$encodedTitle&page=1"

        val response = FlxDispatchers.withIOContext {
            client.request(url = url).execute().fromJson<SearchPageDto<FilmSearchItemDto>>()
        }

        val releaseYear = media.releaseDate?.let {
            java.util.Calendar.getInstance().apply { timeInMillis = it }.get(java.util.Calendar.YEAR)
        }

        val match = response.results.firstOrNull { candidate ->
            val titleMatch = candidate.title?.equals(title, ignoreCase = true) == true ||
                candidate.name?.equals(title, ignoreCase = true) == true
            if (!titleMatch) return@firstOrNull false
            if (releaseYear == null) return@firstOrNull true
            val candidateYear = (candidate.releaseDate ?: candidate.firstAirDate)
                ?.substringBefore('-')?.toIntOrNull()
            candidateYear == null || kotlin.math.abs(candidateYear - releaseYear) <= 1
        } ?: return null

        val partial = match.toPartialMedia(providerId, hint) ?: return null
        return if (partial.type.isMovie) {
            try { fetchMovie(partial.id) } catch (_: Throwable) { partial }
        } else {
            try { fetchShow(partial.id) } catch (_: Throwable) { partial }
        }
    }

    private suspend fun fetchMovie(tmdbId: String): MediaMetadata {
        val imgCfg = settings.readImageConfig()
        val url = "${TMDB_API_BASE_URL}movie/$tmdbId?append_to_response=$APPEND_TO_RESPONSE"
        val dto = FlxDispatchers.withIOContext {
            client.request(url = url).execute().fromJson<MovieDetailDto>()
        }
        return dto.toMovie(providerId, imgCfg)
    }

    private suspend fun fetchShow(tmdbId: String): MediaMetadata {
        val imgCfg = settings.readImageConfig()
        val url = "${TMDB_API_BASE_URL}tv/$tmdbId?append_to_response=$APPEND_TO_RESPONSE"
        val showDto = FlxDispatchers.withIOContext {
            client.request(url = url).execute().fromJson<TvShowDetailDto>()
        }
        val seasons = coroutineScope {
            showDto.seasons
                .filter { it.seasonNumber > 0 }
                .map { brief ->
                    async {
                        try {
                            val seasonUrl = "${TMDB_API_BASE_URL}tv/$tmdbId/season/${brief.seasonNumber}"
                            FlxDispatchers.withIOContext {
                                client.request(url = seasonUrl).execute().fromJson<SeasonDto>().toSeason()
                            }
                        } catch (_: Throwable) { null }
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
        return showDto.toShow(providerId, seasons, imgCfg)
    }
}
