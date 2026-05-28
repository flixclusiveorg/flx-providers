package com.flixclusive.provider.app.tmdb.feature.search

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.provider.app.tmdb.core.config.TMDB_API_BASE_URL
import com.flixclusive.provider.app.tmdb.core.config.readImageConfig
import com.flixclusive.provider.app.tmdb.core.model.dto.FilmSearchItemDto
import com.flixclusive.provider.app.tmdb.core.model.dto.SearchPageDto
import com.flixclusive.provider.capability.SearchProviderApi
import com.flixclusive.provider.filter.Filter
import com.flixclusive.provider.filter.FilterGroup
import com.flixclusive.provider.filter.FilterList
import okhttp3.OkHttpClient
import java.net.URLEncoder

internal class TMDBSearchProvider(
    private val client: OkHttpClient,
    private val settings: DataStore<Preferences>,
    private val providerId: String,
) : SearchProviderApi {

    override val filters: FilterList
        get() = FilterList(
            FilterGroup(
                "Media Type",
                Filter.Select("Media Type", listOf("All", "Movies", "TV Shows"), 0),
            ),
        )

    override suspend fun search(
        query: String,
        page: Int,
        filters: FilterList,
    ): PaginatedMedia<PartialMedia> {
        if (query.isBlank()) {
            return PaginatedMedia(page = 1, results = emptyList(), hasNextPage = false)
        }

        val mediaTypeFilter = (filters[0][0] as? Filter.Select<*>)?.state ?: 0
        val (endpoint, hint) = when (mediaTypeFilter) {
            1 -> "search/movie" to "movie"
            2 -> "search/tv" to "tv"
            else -> "search/multi" to null
        }

        val imgCfg = settings.readImageConfig()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "${TMDB_API_BASE_URL}${endpoint}?query=$encodedQuery&page=$page"

        val response = FlxDispatchers.withIOContext {
            client.request(url = url)
                .execute()
                .fromJson<SearchPageDto<FilmSearchItemDto>>()
        }

        val results = response.results.mapNotNull { it.toPartialMedia(providerId, hint, imgCfg) }
        return PaginatedMedia(
            page = response.page,
            results = results,
            hasNextPage = response.page < response.totalPages,
            totalPages = response.totalPages,
        )
    }
}
