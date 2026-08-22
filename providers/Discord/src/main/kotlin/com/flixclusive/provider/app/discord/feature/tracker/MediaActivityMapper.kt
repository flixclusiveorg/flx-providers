package com.flixclusive.provider.app.discord.feature.tracker

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.provider.app.discord.core.config.DiscordConfig
import com.flixclusive.provider.app.discord.core.model.DiscordActivity
import com.flixclusive.provider.app.discord.core.model.DiscordButton

private const val MINUTE_IN_MS = 60_000L

internal fun MediaMetadata.toDiscordActivity(
    episode: Episode?,
    progressPercent: Float,
): DiscordActivity {
    val span =
        (episode?.runtime ?: runtime)?.times(MINUTE_IN_MS)?.let { runtimeMs ->
            val elapsedMs = (runtimeMs * progressPercent.coerceIn(0f, 100f) / 100f).toLong()
            val startedAt = System.currentTimeMillis() - elapsedMs
            startedAt to startedAt + runtimeMs
        }

    return DiscordActivity(
        name = "$title - ${DiscordConfig.APP_NAME}",
        details = title,
        state = episode?.toStateLine(),
        startTimestamp = span?.first,
        endTimestamp = span?.second,
        largeImage = posterImage ?: logoImage ?: backdropImage,
        largeText = title,
        buttons =
            listOf(
                DiscordButton(
                    label = DiscordConfig.DOWNLOAD_BUTTON_LABEL,
                    url = DiscordConfig.DOWNLOAD_URL,
                ),
            ),
    )
}

private fun Episode.toStateLine(): String {
    val label = "S$season:E$number"
    return title?.takeIf(String::isNotBlank)?.let { "$label — $it" } ?: label
}
