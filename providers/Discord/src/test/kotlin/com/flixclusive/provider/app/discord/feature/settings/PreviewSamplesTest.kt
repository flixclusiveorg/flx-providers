package com.flixclusive.provider.app.discord.feature.settings

import com.flixclusive.provider.app.discord.core.config.DiscordConfig
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.endsWith
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

class PreviewSamplesTest {
    @Test
    fun `movie sample fills the card without an episode line`() {
        val activity = sampleActivity(PreviewKind.MOVIE)

        expectThat(activity.name).endsWith("- ${DiscordConfig.APP_NAME}")
        expectThat(activity.details).isEqualTo("The Matrix")
        expectThat(activity.state).isNull()
        expectThat(activity.largeImage).isNotNull()
    }

    @Test
    fun `show sample carries an episode line`() {
        val activity = sampleActivity(PreviewKind.SHOW)

        expectThat(activity.details).isEqualTo("Breaking Bad")
        expectThat(activity.state).isEqualTo("S2:E4 — Down")
    }

    @Test
    fun `both samples show the download button and a progress window`() {
        PreviewKind.entries.forEach { kind ->
            val activity = sampleActivity(kind)

            expectThat(activity.buttons.map { it.label }).contains(DiscordConfig.DOWNLOAD_BUTTON_LABEL)
            expectThat(activity.startTimestamp).isNotNull()
            expectThat(activity.endTimestamp).isNotNull()
        }
    }
}
