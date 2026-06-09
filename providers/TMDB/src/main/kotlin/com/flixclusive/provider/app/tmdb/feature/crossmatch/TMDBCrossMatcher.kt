package com.flixclusive.provider.app.tmdb.feature.crossmatch

import androidx.compose.ui.util.fastFirstOrNull
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.provider.app.tmdb.core.config.APPEND_TO_RESPONSE
import com.flixclusive.provider.app.tmdb.core.config.TMDB_API_BASE_URL
import com.flixclusive.provider.app.tmdb.core.config.readImageConfig
import com.flixclusive.provider.app.tmdb.core.model.dto.FilmSearchItemDto
import com.flixclusive.provider.app.tmdb.core.model.dto.MovieDetailDto
import com.flixclusive.provider.app.tmdb.core.model.dto.SearchPageDto
import com.flixclusive.provider.app.tmdb.core.model.dto.TvShowDetailDto
import com.flixclusive.provider.capability.CrossMatchProviderApi
import okhttp3.OkHttpClient
import java.net.URLEncoder

internal class TMDBCrossMatcher(
    private val client: OkHttpClient,
    private val settings: DataStore<Preferences>,
    private val providerId: String,
) : CrossMatchProviderApi {

    override suspend fun getById(
        mediaType: MediaType,
        sourceIds: Map<MediaIdSource, String>
    ): MediaMetadata? {
        val tmdbId = sourceIds[MediaIdSource.TMDB] ?: return null

        return safeCall {
            when (mediaType) {
                MediaType.MOVIE -> fetchMovie(tmdbId)
                MediaType.SHOW -> fetchShow(tmdbId)
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
        val encodedTitle = FlxDispatchers.withIOContext {
            URLEncoder.encode(title, "UTF-8")
        }
        val url = "${TMDB_API_BASE_URL}${endpoint}?query=$encodedTitle&page=1"

        val response = FlxDispatchers.withIOContext {
            client.request(url = url).execute().fromJson<SearchPageDto<FilmSearchItemDto>>()
        }

        val releaseYear = media.releaseDate?.let {
            java.util.Calendar.getInstance().apply { timeInMillis = it }.get(java.util.Calendar.YEAR)
        }

        val match = response.results.fastFirstOrNull { candidate ->
            val titleMatch = candidate.title?.equals(title, ignoreCase = true) == true ||
                candidate.name?.equals(title, ignoreCase = true) == true
            if (!titleMatch) return@fastFirstOrNull false
            if (releaseYear == null) return@fastFirstOrNull true
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

        return showDto.toShow(providerId, imgCfg)
    }
}
