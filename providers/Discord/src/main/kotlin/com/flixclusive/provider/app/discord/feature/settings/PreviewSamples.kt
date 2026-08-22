package com.flixclusive.provider.app.discord.feature.settings

import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.provider.app.discord.core.model.DiscordActivity
import com.flixclusive.provider.app.discord.feature.tracker.toDiscordActivity

internal enum class PreviewKind(val label: String) {
    MOVIE("Movie"),
    SHOW("Show"),
}

private const val SAMPLE_PROVIDER_ID = "preview"
private const val SAMPLE_POSTER = "https://image.tmdb.org/t/p/original/dXNAPwY7VrqMAo51EKhhCJfaGb5.jpg"
private const val SAMPLE_SHOW_POSTER = "https://media.themoviedb.org/t/p/w440_and_h660_face/anFx9aTOOYqgS3v7x3R84Kz67ly.jpg"

internal fun sampleActivity(kind: PreviewKind): DiscordActivity =
    when (kind) {
        PreviewKind.MOVIE ->
            Movie(
                id = "sample-movie",
                providerId = SAMPLE_PROVIDER_ID,
                title = "The Matrix",
                posterImage = SAMPLE_POSTER,
                runtime = 136,
            ).toDiscordActivity(episode = null, progressPercent = 35f)

        PreviewKind.SHOW ->
            Show(
                id = "sample-show",
                providerId = SAMPLE_PROVIDER_ID,
                title = "Breaking Bad",
                posterImage = SAMPLE_SHOW_POSTER,
                seasons = emptyList(),
                totalEpisodes = 62,
                totalSeasons = 5,
            ).toDiscordActivity(
                episode =
                    Episode(
                        id = "sample-episode",
                        number = 4,
                        season = 2,
                        isReleased = true,
                        title = "Down",
                        runtime = 47,
                    ),
                progressPercent = 35f,
            )
    }
