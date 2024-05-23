package com.flixProviders.flixhq

import com.flxProviders.flixhq.api.FlixHQApi
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class FlixHQApiTest {

    private lateinit var flixHQApi: FlixHQApi

    @Before
    fun setUp() {
        flixHQApi = FlixHQApi(OkHttpClient())
    }

    @Test
    fun getEpisodeId_fetchesAndReturnsEpisodeId() {
        // Arrange
        val filmId = "watch-shogun-assassin-7825"
        val episode = 1
        val season = 1

        // Act
        val episodeId = flixHQApi.getEpisodeId(filmId, episode, season)

        // Assert
        println(episodeId)
        assertNotNull(episodeId)
    }
}