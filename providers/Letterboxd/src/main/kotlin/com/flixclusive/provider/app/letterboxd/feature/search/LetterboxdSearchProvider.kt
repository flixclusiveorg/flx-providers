package com.flixclusive.provider.app.letterboxd.feature.search

import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.provider.app.letterboxd.core.config.LetterboxdConfig
import com.flixclusive.provider.app.letterboxd.core.model.dto.SearchResponseDto
import com.flixclusive.provider.app.letterboxd.core.model.dto.toPartialMedia
import com.flixclusive.provider.app.letterboxd.core.network.CursorLedger
import com.flixclusive.provider.app.letterboxd.core.network.LetterboxdApi
import com.flixclusive.provider.app.letterboxd.core.network.paged
import com.flixclusive.provider.capability.SearchProviderApi
import com.flixclusive.provider.filter.FilterList

internal class LetterboxdSearchProvider(
    private val api: LetterboxdApi,
    private val providerId: String,
) : SearchProviderApi {
    private val ledger = CursorLedger()

    override val filters = FilterList()

    override suspend fun search(
        query: String,
        page: Int,
        filters: FilterList,
    ): PaginatedMedia<PartialMedia> {
        if (query.isBlank()) {
            return PaginatedMedia(page = page, results = emptyList(), hasNextPage = false, totalPages = page)
        }

        return ledger.paged(key = query, page = page) { cursor ->
            val params =
                buildList {
                    add("input" to query)
                    add("include" to "FilmSearchItem")
                    add("perPage" to LetterboxdConfig.MAX_PER_PAGE.toString())
                    cursor?.let { add("cursor" to it) }
                }

            val dto = fromJson<SearchResponseDto>(api.get(path = "search", params = params))
            dto.next to dto.items.mapNotNull { it.film?.toPartialMedia(providerId) }
        }
    }

    /** Shared with cross-matching, which needs raw candidates rather than a page. */
    internal suspend fun candidates(query: String): List<PartialMedia> {
        if (query.isBlank()) return emptyList()

        val params =
            listOf(
                "input" to query,
                "include" to "FilmSearchItem",
                "perPage" to "20",
            )

        val dto = fromJson<SearchResponseDto>(api.get(path = "search", params = params))
        return dto.items.mapNotNull { it.film?.toPartialMedia(providerId) }
    }
}
