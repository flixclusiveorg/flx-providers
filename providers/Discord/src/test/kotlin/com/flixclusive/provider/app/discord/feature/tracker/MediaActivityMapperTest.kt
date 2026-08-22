package com.flixclusive.provider.app.discord.feature.tracker

import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.provider.app.discord.core.config.DiscordConfig
import com.flixclusive.provider.app.discord.core.model.DiscordActivity
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

private fun media(
    title: String = "The Matrix",
    poster: String? = "https://image.tmdb.org/t/p/w500/poster.jpg",
) = PartialMedia(
    type = MediaType.MOVIE,
    id = "1",
    title = title,
    providerId = "flx-discord",
    posterImage = poster,
)

class MediaActivityMapperTest {
    @Test
    fun `brands the watching label but keeps details as the plain title`() {
        val activity = media().toDiscordActivity(episode = null, progressPercent = 0f)

        expectThat(activity.name).isEqualTo("The Matrix - Flixclusive")
        expectThat(activity.details).isEqualTo("The Matrix")
        expectThat(activity.type).isEqualTo(DiscordActivity.TYPE_WATCHING)
    }

    @Test
    fun `carries a single download button`() {
        val activity = media().toDiscordActivity(episode = null, progressPercent = 0f)

        expectThat(activity.buttons).hasSize(1)
        expectThat(activity.buttons.first().label).isEqualTo(DiscordConfig.DOWNLOAD_BUTTON_LABEL)
        expectThat(activity.buttons.first().url).isEqualTo(DiscordConfig.DOWNLOAD_URL)
    }

    @Test
    fun `labels an episode in the state line`() {
        val episode = Episode(id = "e", number = 3, season = 1, isReleased = true, title = "Pilot")

        val activity = media().toDiscordActivity(episode = episode, progressPercent = 0f)

        expectThat(activity.state).isEqualTo("S1:E3 — Pilot")
    }

    @Test
    fun `falls back to the bare label when an episode has no title`() {
        val episode = Episode(id = "e", number = 3, season = 1, isReleased = true)

        val activity = media().toDiscordActivity(episode = episode, progressPercent = 0f)

        expectThat(activity.state).isEqualTo("S1:E3")
    }

    @Test
    fun `has no state line for a movie`() {
        val activity = media().toDiscordActivity(episode = null, progressPercent = 0f)

        expectThat(activity.state).isNull()
    }

    @Test
    fun `passes the poster through for later asset resolution`() {
        val activity = media().toDiscordActivity(episode = null, progressPercent = 0f)

        expectThat(activity.largeImage).isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg")
        expectThat(activity.largeText).isEqualTo("The Matrix")
    }

    @Test
    fun `omits timestamps when no runtime is known`() {
        val activity = media().toDiscordActivity(episode = null, progressPercent = 50f)

        expectThat(activity.startTimestamp).isNull()
        expectThat(activity.endTimestamp).isNull()
    }

    @Test
    fun `derives a window from the episode runtime`() {
        val episode = Episode(id = "e", number = 1, season = 1, isReleased = true, runtime = 60)

        val activity = media().toDiscordActivity(episode = episode, progressPercent = 50f)

        val start = requireNotNull(activity.startTimestamp)
        val end = requireNotNull(activity.endTimestamp)

        expectThat(end - start).isEqualTo(3_600_000L)
    }
}
