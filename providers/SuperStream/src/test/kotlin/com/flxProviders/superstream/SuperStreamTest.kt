package com.flxProviders.superstream

import com.flixclusive.core.util.log.LogRule
import com.flixclusive.core.util.log.debugLog
import com.flixclusive.model.film.FilmDetails
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.link.MediaLink
import com.flxProviders.superstream.api.SuperStreamApi
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test


class SuperStreamTest {
    @get:Rule
    val logRule = LogRule()
    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = TestScope(UnconfinedTestDispatcher())

    private lateinit var api: SuperStreamApi

    private val movie = Movie(
        tmdbId = 299534,
        imdbId = "tt4154796",
        title = "Avengers: Endgame",
        posterImage = null,
        homePage = null,
        id = null,
        providerName = "TMDB"
    )

    private val tvShow = TvShow(
        tmdbId = 94605,
        imdbId = "tt11126994",
        title = "Arcane",
        posterImage = null,
        homePage = null,
        id = null,
        providerName = "TMDB"
    )

    @Before
    fun setUp() {
        api = mockk<SuperStreamApi> {

        }
    }

    @Test
    fun `get movie links`() = scope.runTest {
        val watchId = getWatchId(movie)
        assertNotNull(watchId) {
            "WatchId cannot be null"
        }
        debugLog("WatchId: $watchId")

        val links = mutableListOf<MediaLink>()
        api.getLinks(
            watchId = watchId!!,
            film = movie,
            onLinkFound = links::add
        )

        debugLog("Link: $links")
        assert(links.size > 0) {
            "No links loaded"
        }
    }

    @Test
    fun `get tv show links`() = scope.runTest {
        val watchId = getWatchId(tvShow)
        assertNotNull(watchId) {
            "WatchId cannot be null"
        }
        debugLog("WatchId: $watchId")

        val links = mutableListOf<MediaLink>()
        api.getLinks(
            watchId = watchId!!,
            film = tvShow,
            episode = Episode(season = 1, number = 1),
            onLinkFound = links::add
        )

        debugLog("Link: $links")
        assert(links.size > 0) {
            "No links loaded"
        }
    }

    @Test
    fun `search for Avengers Endgame`() = scope.runTest {
        val response = api.search(
            title = movie.title,
            imdbId = movie.imdbId,
        )

        assert(response.results.isNotEmpty()) {
            "No search results found for ${movie.title}"
        }

        debugLog("Search results: ${response.results}")
    }

    private suspend fun getWatchId(film: FilmDetails): String? {
        return api.search(
            title = film.title,
            imdbId = film.imdbId,
        ).results.firstOrNull()?.id
    }
}