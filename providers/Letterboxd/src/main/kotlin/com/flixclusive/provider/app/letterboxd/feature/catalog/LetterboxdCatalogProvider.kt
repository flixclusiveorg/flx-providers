package com.flixclusive.provider.app.letterboxd.feature.catalog

import com.flixclusive.core.util.log.debugLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.model.provider.Catalog
import com.flixclusive.provider.app.letterboxd.core.config.LetterboxdConfig
import com.flixclusive.provider.app.letterboxd.core.model.dto.FilmsResponseDto
import com.flixclusive.provider.app.letterboxd.core.model.dto.ListEntriesResponseDto
import com.flixclusive.provider.app.letterboxd.core.model.dto.ListSummaryDto
import com.flixclusive.provider.app.letterboxd.core.model.dto.ListsResponseDto
import com.flixclusive.provider.app.letterboxd.core.model.dto.LogEntriesResponseDto
import com.flixclusive.provider.app.letterboxd.core.model.dto.bestUrl
import com.flixclusive.provider.app.letterboxd.core.model.dto.toPartialMedia
import com.flixclusive.provider.app.letterboxd.core.network.CursorLedger
import com.flixclusive.provider.app.letterboxd.core.network.LetterboxdApi
import com.flixclusive.provider.app.letterboxd.core.network.LetterboxdTokenProvider
import com.flixclusive.provider.app.letterboxd.core.network.paged
import com.flixclusive.provider.capability.CatalogProviderApi

private const val MAX_LIST_CATALOGS = 10
private const val LIST_PREVIEW_POSTER_WIDTH = 300

private val PUBLIC_CHARTS =
    listOf(
        Triple("Popular This Week", "FilmPopularityThisWeek", "The films Letterboxd members are watching right now."),
        Triple("Popular All-Time", "FilmPopularity", "The most-watched films on Letterboxd."),
        Triple("Highest Rated", "AverageRatingHighToLow", "Films with the highest member ratings."),
        Triple("Recently Released", "ReleaseDateLatestFirst", "The newest releases."),
    )

internal class LetterboxdCatalogProvider(
    private val api: LetterboxdApi,
    private val tokens: LetterboxdTokenProvider,
    private val providerId: String,
) : CatalogProviderApi {
    private val ledger = CursorLedger()

    override suspend fun getCatalogs(): List<Catalog> =
        buildList {
            PUBLIC_CHARTS.forEach { (name, sort, description) ->
                add(
                    Catalog(
                        name = name,
                        url = CatalogRoute.Films(sort).encode(),
                        providerId = providerId,
                        canPaginate = true,
                        description = description,
                    ),
                )
            }

            // Member catalogs are simply absent when signed out rather than failing.
            val member = tokens.profile()
            if (member != null) {
                add(
                    Catalog(
                        name = "Your Watchlist",
                        url = CatalogRoute.Watchlist(member.id).encode(),
                        providerId = providerId,
                        canPaginate = true,
                        description = "Films you have saved on Letterboxd.",
                    ),
                )
                add(
                    Catalog(
                        name = "Your Diary",
                        url = CatalogRoute.Diary(member.id).encode(),
                        providerId = providerId,
                        canPaginate = true,
                        description = "Films you have logged on Letterboxd.",
                    ),
                )

                addAll(memberListCatalogs(member.id))
            }

            addAll(popularListCatalogs())
        }

    override suspend fun getCatalogItems(
        catalog: Catalog,
        page: Int,
    ): PaginatedMedia<PartialMedia> {
        val route =
            decodeCatalogRoute(catalog.url)
                ?: throw IllegalArgumentException("Unrecognised Letterboxd catalog: ${catalog.url}")

        return ledger.paged(key = catalog.url, page = page) { cursor ->
            when (route) {
                is CatalogRoute.Films -> fetchFilms(route.sort, cursor)
                is CatalogRoute.Watchlist -> fetchWatchlist(route.memberId, cursor)
                is CatalogRoute.Diary -> fetchDiary(route.memberId, cursor)
                is CatalogRoute.FilmList -> fetchListEntries(route.listId, cursor)
            }
        }
    }

    private fun pageParams(cursor: String?): List<Pair<String, String>> =
        buildList {
            add("perPage" to LetterboxdConfig.MAX_PER_PAGE.toString())
            cursor?.let { add("cursor" to it) }
        }

    private suspend fun fetchFilms(
        sort: String,
        cursor: String?,
    ): Pair<String?, List<PartialMedia>> {
        val body = api.get(path = "films", params = pageParams(cursor) + ("sort" to sort))
        val dto = fromJson<FilmsResponseDto>(body)
        return dto.next to dto.items.mapNotNull { it.toPartialMedia(providerId) }
    }

    private suspend fun fetchWatchlist(
        memberId: String,
        cursor: String?,
    ): Pair<String?, List<PartialMedia>> {
        val body = api.get(path = "member/$memberId/watchlist", params = pageParams(cursor), requireMember = true)
        val dto = fromJson<FilmsResponseDto>(body)
        return dto.next to dto.items.mapNotNull { it.toPartialMedia(providerId) }
    }

    private suspend fun fetchDiary(
        memberId: String,
        cursor: String?,
    ): Pair<String?, List<PartialMedia>> {
        val params = pageParams(cursor) + ("member" to memberId)
        val body = api.get(path = "log-entries", params = params, requireMember = true)
        val dto = fromJson<LogEntriesResponseDto>(body)
        return dto.next to dto.items.mapNotNull { it.film?.toPartialMedia(providerId) }
    }

    private suspend fun fetchListEntries(
        listId: String,
        cursor: String?,
    ): Pair<String?, List<PartialMedia>> {
        val body = api.get(path = "list/$listId/entries", params = pageParams(cursor))
        val dto = fromJson<ListEntriesResponseDto>(body)
        return dto.next to dto.items.mapNotNull { it.film?.toPartialMedia(providerId) }
    }

    private suspend fun memberListCatalogs(memberId: String): List<Catalog> =
        runCatching {
            val body =
                api.get(
                    path = "lists",
                    params = listOf("member" to memberId, "perPage" to MAX_LIST_CATALOGS.toString()),
                    requireMember = true,
                )
            fromJson<ListsResponseDto>(body).toCatalogs(prefix = null)
        }.getOrElse {
            debugLog("Letterboxd: could not load your lists — ${it.message}")
            emptyList()
        }

    private suspend fun popularListCatalogs(): List<Catalog> =
        runCatching {
            val body = api.get(path = "lists", params = listOf("perPage" to MAX_LIST_CATALOGS.toString()))
            fromJson<ListsResponseDto>(body).toCatalogs(prefix = "Letterboxd")
        }.getOrElse {
            debugLog("Letterboxd: could not load community lists — ${it.message}")
            emptyList()
        }

    private fun ListsResponseDto.toCatalogs(prefix: String?): List<Catalog> {
        if (items.size > MAX_LIST_CATALOGS) {
            debugLog("Letterboxd: showing $MAX_LIST_CATALOGS of ${items.size} lists; the rest were dropped.")
        }

        return items.take(MAX_LIST_CATALOGS).mapNotNull { it.toCatalog(prefix) }
    }

    private fun ListSummaryDto.toCatalog(prefix: String?): Catalog? {
        if (id.isBlank() || name.isBlank()) return null

        return Catalog(
            name = if (prefix == null) name else "$prefix · $name",
            url = CatalogRoute.FilmList(id).encode(),
            providerId = providerId,
            canPaginate = true,
            description = description,
            image = previewEntries.firstNotNullOfOrNull { it.film?.poster.bestUrl(LIST_PREVIEW_POSTER_WIDTH) },
        )
    }
}
