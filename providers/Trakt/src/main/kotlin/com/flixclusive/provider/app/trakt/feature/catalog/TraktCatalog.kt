package com.flixclusive.provider.app.trakt.feature.catalog

import android.content.Context
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.model.provider.Catalog
import com.flixclusive.provider.app.trakt.BuildConfig
import com.flixclusive.provider.capability.CatalogProviderApi
import com.flixclusive.provider.app.trakt.TraktPlugin
import com.flixclusive.provider.app.trakt.core.config.TraktApiConfig
import com.flixclusive.provider.app.trakt.core.model.AuthToken
import com.flixclusive.provider.app.trakt.core.model.TraktMedia.Companion.toPartialMedia
import com.flixclusive.provider.app.trakt.core.network.TraktApiService
import com.flixclusive.provider.app.trakt.core.network.util.OkHttpClientUtil
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.URLEncoder
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TraktCatalog internal constructor(
    private val context: Context,
    private val plugin: TraktPlugin,
    private val authToken: AuthToken,
) : CatalogProviderApi {
    private val apiService by lazy {
        TraktApiService.create(
            OkHttpClientUtil.createNonCachedClient(plugin.settings)
        )
    }

    private val defaultAuthHeaders by lazy {
        mapOf(
            "authorization" to "Bearer ${authToken.accessToken}",
            "trakt-api-version" to "2",
            "trakt-api-key" to BuildConfig.TRAKT_CLIENT_ID
        )
    }

    override suspend fun getCatalogItems(
        catalog: Catalog,
        page: Int
    ): PaginatedMedia<PartialMedia> {
        val cachedApiService = TraktApiService.create(
            OkHttpClientUtil.createCachedClient(
                context = context,
                settings = plugin.settings,
                cacheMaxAge = 60 * 60 * 8
            )
        )

        val url = "${catalog.url}&page=$page&limit=${TraktApiConfig.PAGE_RESULTS_LIMIT}"
        val items = if (catalog.url.contains("/media/popular?")) {
            cachedApiService.getPopularCatalogItems(url)
                .fastMap { it.toPartialMedia(plugin.id) }
        } else {
            cachedApiService.getCatalogItems(url)
                .fastMap { it.toPartialMedia(plugin.id) }
        }

        return PaginatedMedia(
            page = page,
            hasNextPage = items.size == TraktApiConfig.PAGE_RESULTS_LIMIT,
            results = items
        )
    }

    override suspend fun getCatalogs(): List<Catalog> {
        val deferred = mutableListOf<Deferred<List<Catalog>>>()
        val catalogs = mutableListOf<Catalog>()

        coroutineScope {
            val movieSmartLists = async {
                apiService.getSavedFilters(type = "movies").fastMap {
                    it.toCatalog(
                        providerId = plugin.id,
                        authToken = authToken.accessToken
                    )
                }
            }

            val showSmartLists = async {
                apiService.getSavedFilters(type = "shows").fastMap {
                    it.toCatalog(
                        providerId = plugin.id,
                        authToken = authToken.accessToken
                    )
                }
            }

            val trendingAsync = async { listOf(getTrendingCatalog()) }
            val popularAsync = async { getPopularCatalogs() }
            val anticipatedAsync = async { listOf(getAnticipatedCatalog()) }
            val likedListsAsync = async { getLikedListsAsCatalogs() }
            val recommendedAsync = async {
                val recommendedCatalog = getRecommendedCatalog()
                val results = safeCall {
                    apiService.getCatalogItems(recommendedCatalog.url)
                } ?: emptyList()

                if (results.isEmpty()) emptyList() else listOf(recommendedCatalog)
            }
            val watchlistAsync = async {
                val watchlist = getWatchlistCatalog()
                val results = safeCall {
                    apiService.getCatalogItems(watchlist.url)
                } ?: emptyList()

                if (results.isEmpty()) emptyList() else listOf(watchlist)
            }

            deferred.addAll(
                listOf(
                    trendingAsync,
                    popularAsync,
                    anticipatedAsync,
                    likedListsAsync,
                    recommendedAsync,
                    watchlistAsync,
                    movieSmartLists,
                    showSmartLists
                )
            )

            deferred.awaitAll()
                .fastForEach(catalogs::addAll)
        }

        return catalogs
    }

    private fun getTrendingCatalog(): Catalog {
        return Catalog(
            name = "Trending",
            description = "The most popular movies and shows on Trakt right now.",
            providerId = plugin.id,
            url = "${TraktApiConfig.API_BASE_URL}/media/trending?extended=full%2Cimages",
            canPaginate = true,
            headers = defaultAuthHeaders
        )
    }

    private fun getRecommendedCatalog(): Catalog {
        return Catalog(
            name = "Recommended",
            description = "Personalized recommendations based on your watch history and ratings.",
            providerId = plugin.id,
            headers = defaultAuthHeaders,
            canPaginate = true,
            url = "${TraktApiConfig.API_BASE_URL}/media/recommendations" +
                    "?extended=full%2Cimages" +
                    "&ignore_collected=true" +
                    "&watch_window=25" +
                    "&ignore_watched=true"
        )
    }

    private fun getWatchlistCatalog(): Catalog {
        return Catalog(
            name = "Trakt Watchlist",
            providerId = plugin.id,
            headers = defaultAuthHeaders,
            canPaginate = true,
            url = "${TraktApiConfig.API_BASE_URL}/users/me/watchlist/movie,show" +
                    "?extended=full%2Cimages" +
                    "&ignore_collected=true" +
                    "&watch_window=25" +
                    "&ignore_watched=true" +
                    "&sort_by=added" +
                    "&sort_how=desc"
        )
    }

    private fun getPopularCatalogs(): List<Catalog> {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val formatter = DateTimeFormatter.ISO_INSTANT

        val startOfToday = now.minusDays(1)

        fun buildUrl(startDate: ZonedDateTime): String {
            val start = URLEncoder.encode(formatter.format(startDate), "UTF-8")
            val end = URLEncoder.encode(formatter.format(now), "UTF-8")
            return "${TraktApiConfig.API_BASE_URL}/media/popular" +
                    "?extended=full%2Cimages" +
                    "&start_date=$start" +
                    "&end_date=$end"
        }

        return listOf(
            Catalog(
                name = "Popular",
                description = "The most popular movies and shows on Trakt over the past 24 hours.",
                providerId = plugin.id,
                canPaginate = true,
                headers = defaultAuthHeaders,
                url = buildUrl(startOfToday)
            ),
        )
    }

    private fun getAnticipatedCatalog(): Catalog {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val formatter = DateTimeFormatter.ISO_INSTANT

        val end = URLEncoder.encode(formatter.format(now), "UTF-8")

        return Catalog(
            name = "Anticipated",
            description = "The most anticipated movies and shows coming soon, based on Trakt user activity.",
            providerId = plugin.id,
            canPaginate = true,
            headers = defaultAuthHeaders,
            url = "${TraktApiConfig.API_BASE_URL}/media/anticipated" +
                    "?extended=full%2Cimages" +
                    "&end_date=$end"
        )
    }

    private suspend fun getLikedListsAsCatalogs(): List<Catalog> {
        return apiService.getLikedLists(
            page = 1,
            limit = 100
        ).fastMap {
            it.list.toCatalog(
                providerId = plugin.id,
                authToken = authToken.accessToken
            )
        }
    }
}