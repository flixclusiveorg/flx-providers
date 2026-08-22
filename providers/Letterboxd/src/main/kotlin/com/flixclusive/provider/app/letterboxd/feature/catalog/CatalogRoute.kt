package com.flixclusive.provider.app.letterboxd.feature.catalog

/**
 * `Catalog.url` is opaque to the host and round-trips back into `getCatalogItems`, so it
 * carries the query rather than a real address.
 */
internal sealed interface CatalogRoute {
    data class Films(val sort: String) : CatalogRoute

    data class Watchlist(val memberId: String) : CatalogRoute

    data class Diary(val memberId: String) : CatalogRoute

    data class FilmList(val listId: String) : CatalogRoute

    fun encode(): String =
        when (this) {
            is Films -> "$SCHEME/films/$sort"
            is Watchlist -> "$SCHEME/watchlist/$memberId"
            is Diary -> "$SCHEME/diary/$memberId"
            is FilmList -> "$SCHEME/list/$listId"
        }
}

private const val SCHEME = "letterboxd://catalog"

internal fun decodeCatalogRoute(url: String): CatalogRoute? {
    if (!url.startsWith("$SCHEME/")) return null

    val rest = url.removePrefix("$SCHEME/")
    val kind = rest.substringBefore('/', missingDelimiterValue = "")
    val value = rest.substringAfter('/', missingDelimiterValue = "")
    if (kind.isBlank() || value.isBlank()) return null

    return when (kind) {
        "films" -> CatalogRoute.Films(value)
        "watchlist" -> CatalogRoute.Watchlist(value)
        "diary" -> CatalogRoute.Diary(value)
        "list" -> CatalogRoute.FilmList(value)
        else -> null
    }
}
