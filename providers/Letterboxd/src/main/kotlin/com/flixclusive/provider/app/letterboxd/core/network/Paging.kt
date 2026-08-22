package com.flixclusive.provider.app.letterboxd.core.network

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.PaginatedMedia

/**
 * Translates one page-indexed request into a cursor-based one. [fetch] receives the
 * cursor for the requested page (null for page 1) and returns the next cursor plus the
 * items it read.
 */
internal suspend fun <T : MediaMetadata> CursorLedger.paged(
    key: String,
    page: Int,
    fetch: suspend (cursor: String?) -> Pair<String?, List<T>>,
): PaginatedMedia<T> {
    val cursor =
        when (val lookup = lookup(key, page)) {
            is CursorLookup.Start -> null
            is CursorLookup.At -> lookup.cursor
            CursorLookup.Exhausted, CursorLookup.Unknown ->
                return PaginatedMedia(page = page, results = emptyList(), hasNextPage = false, totalPages = page)
        }

    val (next, items) = fetch(cursor)
    record(key, page, next)

    val hasNext = !next.isNullOrBlank()
    return PaginatedMedia(
        page = page,
        results = items,
        hasNextPage = hasNext,
        // Letterboxd never reports a total, so this is a lower bound, not a count.
        totalPages = if (hasNext) page + 1 else page,
    )
}
