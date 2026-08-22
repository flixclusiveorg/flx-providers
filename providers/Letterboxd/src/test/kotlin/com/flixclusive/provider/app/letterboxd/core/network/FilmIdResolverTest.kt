package com.flixclusive.provider.app.letterboxd.core.network

import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.MediaType
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

private const val LETTERBOXD = "letterboxd"

private fun media(
    id: String,
    providerId: String = "stremio",
    externalIds: Map<MediaIdSource, String> = emptyMap(),
) = PartialMedia(
    type = MediaType.MOVIE,
    id = id,
    title = "Heat",
    providerId = providerId,
    posterImage = null,
    externalIds = externalIds,
)

class FilmIdResolverTest {
    @Test
    fun `media this provider produced already carries a letterboxd id`() {
        val candidate = media(id = "2bg8", providerId = LETTERBOXD).toLetterboxdCandidate(LETTERBOXD)

        expectThat(candidate).isEqualTo("2bg8")
    }

    @Test
    fun `prefers a tmdb id, which the film path accepts directly`() {
        val candidate =
            media(
                id = "tt0113277",
                externalIds = mapOf(MediaIdSource.TMDB to "949", MediaIdSource.IMDB to "tt0113277"),
            ).toLetterboxdCandidate(LETTERBOXD)

        expectThat(candidate).isEqualTo("tmdb:949")
    }

    @Test
    fun `falls back to an imdb id when there is no tmdb one`() {
        val candidate =
            media(id = "x", externalIds = mapOf(MediaIdSource.IMDB to "tt0113277"))
                .toLetterboxdCandidate(LETTERBOXD)

        expectThat(candidate).isEqualTo("imdb:tt0113277")
    }

    @Test
    fun `treats a bare tt-prefixed id as an imdb id`() {
        expectThat(media(id = "tt0113277").toLetterboxdCandidate(LETTERBOXD)).isEqualTo("imdb:tt0113277")
    }

    @Test
    fun `media with nothing linking it to letterboxd has no candidate`() {
        expectThat(media(id = "some-internal-id").toLetterboxdCandidate(LETTERBOXD)).isNull()
    }

    @Test
    fun `blank external ids are ignored`() {
        val candidate =
            media(id = "x", externalIds = mapOf(MediaIdSource.TMDB to "", MediaIdSource.IMDB to "tt1"))
                .toLetterboxdCandidate(LETTERBOXD)

        expectThat(candidate).isEqualTo("imdb:tt1")
    }

    @Test
    fun `a prefixed id from our own provider is still treated as external`() {
        // Guards against handing "tmdb:949" back to a path that only accepts a real LID.
        val candidate =
            media(id = "tmdb:949", providerId = LETTERBOXD, externalIds = mapOf(MediaIdSource.TMDB to "949"))
                .toLetterboxdCandidate(LETTERBOXD)

        expectThat(candidate).isEqualTo("tmdb:949")
    }
}
