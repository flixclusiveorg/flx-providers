package com.flixclusive.provider.app.letterboxd.core.network

import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.MediaType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

private fun film(id: String) =
    PartialMedia(
        type = MediaType.MOVIE,
        id = id,
        title = "Film $id",
        providerId = "test",
        posterImage = null,
    )

class PagingTest {
    @Test
    fun `page one fetches with no cursor and reports a next page`() =
        runTest {
            val ledger = CursorLedger()
            var seen: String? = "unset"

            val result =
                ledger.paged("films", page = 1) { cursor ->
                    seen = cursor
                    "cursor-2" to listOf(film("a"), film("b"))
                }

            expectThat(seen).isNull()
            expectThat(result.page).isEqualTo(1)
            expectThat(result.results).hasSize(2)
            expectThat(result.hasNextPage).isTrue()
        }

    @Test
    fun `the second page is fetched with the cursor the first one returned`() =
        runTest {
            val ledger = CursorLedger()
            ledger.paged("films", page = 1) { "cursor-2" to listOf(film("a")) }

            var seen: String? = null
            ledger.paged("films", page = 2) { cursor ->
                seen = cursor
                null to listOf(film("b"))
            }

            expectThat(seen).isEqualTo("cursor-2")
        }

    @Test
    fun `a final page reports no next page`() =
        runTest {
            val ledger = CursorLedger()

            val result = ledger.paged("films", page = 1) { null to listOf(film("a")) }

            expectThat(result.hasNextPage).isFalse()
            expectThat(result.totalPages).isEqualTo(1)
        }

    @Test
    fun `an unreachable page yields an empty result without fetching`() =
        runTest {
            val ledger = CursorLedger()
            var fetched = false

            val result =
                ledger.paged<PartialMedia>("films", page = 7) {
                    fetched = true
                    null to emptyList()
                }

            expectThat(fetched).isFalse()
            expectThat(result.results).isEmpty()
            expectThat(result.hasNextPage).isFalse()
        }
}
