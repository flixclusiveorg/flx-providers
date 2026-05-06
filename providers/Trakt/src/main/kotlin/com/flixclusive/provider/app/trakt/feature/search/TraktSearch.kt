package com.flixclusive.provider.app.trakt.feature.search

import android.content.Context
import androidx.compose.ui.util.fastMap
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.provider.capability.SearchProviderApi
import com.flixclusive.provider.filter.Filter
import com.flixclusive.provider.filter.FilterGroup
import com.flixclusive.provider.filter.FilterList
import com.flixclusive.provider.app.trakt.TraktPlugin
import com.flixclusive.provider.app.trakt.core.config.TraktApiConfig.SEARCH_V2_URL
import com.flixclusive.provider.app.trakt.core.config.TypeSenseKeyProvider
import com.flixclusive.provider.app.trakt.core.network.TraktApiService
import com.flixclusive.provider.app.trakt.core.network.dto.request.TraktSearchRequestV2
import com.flixclusive.provider.app.trakt.core.network.util.OkHttpClientUtil

class TraktSearch internal constructor(
    private val context: Context,
    private val plugin: TraktPlugin,
    private val typeSenseKeyProvider: TypeSenseKeyProvider
): SearchProviderApi {
    companion object {
        internal const val FILTER_ALL = 0
        internal const val FILTER_TV_SHOW = 1
        internal const val FILTER_MOVIES = 2
    }

    private val cachedApiService = TraktApiService.create(
        OkHttpClientUtil.createCachedClient(
            context = context,
            settings = plugin.settings,
            cacheMaxAge = 60 * 60 * 24 * 7 // 7 days in seconds
        )
    )

    override suspend fun search(
        query: String,
        page: Int,
        filters: FilterList
    ): PaginatedMedia<PartialMedia> {
        if (typeSenseKeyProvider.typeSenseKey == null) {
            typeSenseKeyProvider.reloadTypeSenseKey()
        }

        val filters = filters.firstOrNull()?.firstOrNull() as? Filter.Select<*>
        val selectedMediaType = filters?.state ?: error("Invalid filter state used for trakt search")

        val limit = 50
        val url = "$SEARCH_V2_URL?q=$query&limit=$limit&page=$page"

        val response = cachedApiService.search(
            apiKey = typeSenseKeyProvider.typeSenseKey!!,
            url = url,
            requestBody = TraktSearchRequestV2(
                searches = buildList {
                    if (selectedMediaType == FILTER_ALL) {
                        add(
                            TraktSearchRequestV2.SearchRequest(
                                collection = "Show",
                            )
                        )
                        add(
                            TraktSearchRequestV2.SearchRequest(
                                collection = "Movie",
                            )
                        )
                    } else {
                        val requestType = when (selectedMediaType) {
                            FILTER_TV_SHOW -> "Show"
                            FILTER_MOVIES -> "Movie"
                            else -> error("Invalid filter state used for trakt search")
                        }

                        add(
                            TraktSearchRequestV2.SearchRequest(
                                collection = requestType,
                            )
                        )
                    }
                }
            )
        )

        return PaginatedMedia(
            results = response.results.fastMap { it.toPartialMedia(plugin.id) },
            page = page,
            hasNextPage = response.hasNextPage,
            totalPages = response.availablePages
        )
    }

    override val filters: FilterList
        get() = FilterList(TraktFilters())

    private class TraktFilters : FilterGroup(
        name = "Media type",
        Filter.Select(
            name = "Media type",
            options = listOf(
                "All",
                "Shows",
                "Movies",
            ),
            state = FILTER_ALL,
        ),
    )
}