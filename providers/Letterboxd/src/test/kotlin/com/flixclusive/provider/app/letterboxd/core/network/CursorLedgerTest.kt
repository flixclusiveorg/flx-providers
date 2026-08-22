package com.flixclusive.provider.app.letterboxd.core.network

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class CursorLedgerTest {
    @Test
    fun `page one always starts a fresh window`() {
        val ledger = CursorLedger()

        expectThat(ledger.lookup("films", 1)).isA<CursorLookup.Start>()
    }

    @Test
    fun `serves the cursor recorded by the previous page`() {
        val ledger = CursorLedger()
        ledger.record("films", page = 1, next = "cursor-2")

        expectThat(ledger.lookup("films", 2)).isEqualTo(CursorLookup.At("cursor-2"))
    }

    @Test
    fun `walks a sequential scroll`() {
        val ledger = CursorLedger()
        ledger.record("films", page = 1, next = "cursor-2")
        ledger.record("films", page = 2, next = "cursor-3")
        ledger.record("films", page = 3, next = "cursor-4")

        expectThat(ledger.lookup("films", 2)).isEqualTo(CursorLookup.At("cursor-2"))
        expectThat(ledger.lookup("films", 3)).isEqualTo(CursorLookup.At("cursor-3"))
        expectThat(ledger.lookup("films", 4)).isEqualTo(CursorLookup.At("cursor-4"))
    }

    @Test
    fun `a null next marks the following page exhausted`() {
        val ledger = CursorLedger()
        ledger.record("films", page = 1, next = "cursor-2")
        ledger.record("films", page = 2, next = null)

        expectThat(ledger.lookup("films", 3)).isA<CursorLookup.Exhausted>()
        expectThat(ledger.lookup("films", 9)).isA<CursorLookup.Exhausted>()
    }

    @Test
    fun `a page beyond what was recorded is unknown rather than a silent restart`() {
        val ledger = CursorLedger()
        ledger.record("films", page = 1, next = "cursor-2")

        expectThat(ledger.lookup("films", 5)).isA<CursorLookup.Unknown>()
    }

    @Test
    fun `an unseen key is unknown past page one`() {
        val ledger = CursorLedger()

        expectThat(ledger.lookup("never-fetched", 2)).isA<CursorLookup.Unknown>()
    }

    @Test
    fun `keys do not bleed into each other`() {
        val ledger = CursorLedger()
        ledger.record("films", page = 1, next = "films-2")
        ledger.record("search", page = 1, next = "search-2")

        expectThat(ledger.lookup("films", 2)).isEqualTo(CursorLookup.At("films-2"))
        expectThat(ledger.lookup("search", 2)).isEqualTo(CursorLookup.At("search-2"))
    }

    @Test
    fun `a re-fetch that finds more pages clears a stale end marker`() {
        val ledger = CursorLedger()
        ledger.record("films", page = 1, next = null)
        expectThat(ledger.lookup("films", 2)).isA<CursorLookup.Exhausted>()

        ledger.record("films", page = 1, next = "cursor-2")

        expectThat(ledger.lookup("films", 2)).isEqualTo(CursorLookup.At("cursor-2"))
    }

    @Test
    fun `forget drops a single window`() {
        val ledger = CursorLedger()
        ledger.record("films", page = 1, next = "cursor-2")
        ledger.record("search", page = 1, next = "search-2")

        ledger.forget("films")

        expectThat(ledger.lookup("films", 2)).isA<CursorLookup.Unknown>()
        expectThat(ledger.lookup("search", 2)).isEqualTo(CursorLookup.At("search-2"))
    }

    @Test
    fun `evicts the least recently used window past the cap`() {
        val ledger = CursorLedger(maxEntries = 2)
        ledger.record("a", page = 1, next = "a-2")
        ledger.record("b", page = 1, next = "b-2")

        // Touch "a" so "b" becomes the eldest.
        ledger.lookup("a", 2)
        ledger.record("c", page = 1, next = "c-2")

        expectThat(ledger.lookup("a", 2)).isEqualTo(CursorLookup.At("a-2"))
        expectThat(ledger.lookup("c", 2)).isEqualTo(CursorLookup.At("c-2"))
        expectThat(ledger.lookup("b", 2)).isA<CursorLookup.Unknown>()
    }
}
