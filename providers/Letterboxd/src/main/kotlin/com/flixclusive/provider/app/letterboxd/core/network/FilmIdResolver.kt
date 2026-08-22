package com.flixclusive.provider.app.letterboxd.core.network

import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.provider.app.letterboxd.core.model.dto.FilmsResponseDto

private const val MAX_CACHED_IDS = 128

/**
 * `GET /film/{id}` accepts a Letterboxd id or a `tmdb:` prefixed one on the **path**, but
 * an `imdb:` prefixed one 404s there — that prefix is only honoured by the `filmId`
 * *query parameter*. Every path-addressed call therefore needs a real Letterboxd id, and
 * `/films?filmId=` is what turns an external id into one.
 */
internal class FilmIdResolver(
    private val api: LetterboxdApi,
) {
    private val cache =
        object : LinkedHashMap<String, String>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > MAX_CACHED_IDS
        }

    @Synchronized
    private fun cached(key: String): String? = cache[key]

    @Synchronized
    private fun remember(
        key: String,
        lid: String,
    ) {
        cache[key] = lid
    }

    /** Resolves a Letterboxd id, or a `tmdb:` / `imdb:` prefixed one, to a Letterboxd id. */
    suspend fun toLetterboxdId(candidate: String): String? {
        if (candidate.isBlank()) return null
        if (!candidate.contains(':')) return candidate

        cached(candidate)?.let { return it }

        val body = api.get(path = "films", params = listOf("filmId" to candidate, "perPage" to "1"))
        val lid = fromJson<FilmsResponseDto>(body).items.firstOrNull()?.id?.takeIf { it.isNotBlank() } ?: return null

        remember(candidate, lid)
        return lid
    }
}

/**
 * The best externally-addressable id for this media, or null when nothing links it to
 * Letterboxd. Media the provider produced itself already carries a Letterboxd id.
 */
internal fun MediaMetadata.toLetterboxdCandidate(letterboxdProviderId: String): String? {
    if (providerId == letterboxdProviderId && id.isNotBlank() && !id.contains(':')) return id

    externalIds[MediaIdSource.TMDB]?.takeIf { it.isNotBlank() }?.let { return "tmdb:$it" }
    externalIds[MediaIdSource.IMDB]?.takeIf { it.isNotBlank() }?.let { return "imdb:$it" }
    if (id.startsWith("tt")) return "imdb:$id"

    return null
}
