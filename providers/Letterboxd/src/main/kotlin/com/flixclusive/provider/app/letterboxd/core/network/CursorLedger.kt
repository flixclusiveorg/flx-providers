package com.flixclusive.provider.app.letterboxd.core.network

internal sealed interface CursorLookup {
    /** Page 1 — start the window with no cursor. */
    data object Start : CursorLookup

    data class At(val cursor: String) : CursorLookup

    /** The window ended before this page; serve an empty result. */
    data object Exhausted : CursorLookup

    /** No cursor recorded for this page, so it cannot be served directly. */
    data object Unknown : CursorLookup
}

/**
 * The SDK pages by index; Letterboxd pages by an opaque `next` cursor. This records
 * the cursor observed for each page so page N can be served from the cursor handed
 * out when page N-1 was fetched.
 *
 * Sequential scrolling — the only way the host actually pages — always hits. A jump
 * past the recorded window returns [CursorLookup.Unknown] so the caller can stop
 * rather than silently restart from page 1.
 */
internal class CursorLedger(
    private val maxEntries: Int = 32,
) {
    private class Entry {
        /** `cursors[i]` fetches page `i + 2`. */
        val cursors = mutableListOf<String>()
        var endPage: Int? = null
    }

    private val entries =
        object : LinkedHashMap<String, Entry>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean = size > maxEntries
        }

    @Synchronized
    fun lookup(
        key: String,
        page: Int,
    ): CursorLookup {
        if (page <= 1) return CursorLookup.Start

        val entry = entries[key] ?: return CursorLookup.Unknown
        entry.endPage?.let { if (page >= it) return CursorLookup.Exhausted }

        val index = page - 2
        return entry.cursors.getOrNull(index)?.let(CursorLookup::At) ?: CursorLookup.Unknown
    }

    /** Records the cursor returned while fetching [page]; a null [next] marks the end. */
    @Synchronized
    fun record(
        key: String,
        page: Int,
        next: String?,
    ) {
        val entry = entries.getOrPut(key) { Entry() }

        if (next.isNullOrBlank()) {
            entry.endPage = page + 1
            return
        }

        val index = page - 1
        while (entry.cursors.size <= index) {
            entry.cursors.add("")
        }
        entry.cursors[index] = next

        entry.endPage?.let { if (it <= page + 1) entry.endPage = null }
    }

    @Synchronized
    fun forget(key: String) {
        entries.remove(key)
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }
}
