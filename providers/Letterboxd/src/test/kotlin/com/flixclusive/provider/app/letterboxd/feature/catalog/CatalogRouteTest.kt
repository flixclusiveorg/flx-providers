package com.flixclusive.provider.app.letterboxd.feature.catalog

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class CatalogRouteTest {
    @Test
    fun `every route survives a round trip through the catalog url`() {
        val routes =
            listOf(
                CatalogRoute.Films("FilmPopularityThisWeek"),
                CatalogRoute.Watchlist("member-1"),
                CatalogRoute.Diary("member-1"),
                CatalogRoute.FilmList("list-42"),
            )

        routes.forEach { route ->
            expectThat(decodeCatalogRoute(route.encode())).isEqualTo(route)
        }
    }

    @Test
    fun `a url from another provider is rejected`() {
        expectThat(decodeCatalogRoute("https://example.com/films")).isNull()
        expectThat(decodeCatalogRoute("stremio://catalog/films/top")).isNull()
    }

    @Test
    fun `an unknown or incomplete route is rejected`() {
        expectThat(decodeCatalogRoute("letterboxd://catalog/reviews/abc")).isNull()
        expectThat(decodeCatalogRoute("letterboxd://catalog/films/")).isNull()
        expectThat(decodeCatalogRoute("letterboxd://catalog/films")).isNull()
    }
}
