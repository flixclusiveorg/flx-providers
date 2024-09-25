package com.flxProviders.stremio.api.util

import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.model.provider.link.SubtitleSource
import okhttp3.OkHttpClient

internal object OpenSubtitlesUtil {
    private const val OPEN_SUBS_STREMIO_ENDPOINT = "https://opensubtitles-v3.strem.io"

    private data class OpenSubtitleStremioDto(
        val subtitles: List<Map<String, String>>,
    )

    fun OkHttpClient.fetchSubtitles(
        imdbId: String,
        season: Int? = null,
        episode: Int? = null,
        onLinkFound: (MediaLink) -> Unit
    ) {
        if (!imdbId.startsWith("tt"))
            return

        val slug = if(season == null) {
            "movie/$imdbId"
        } else {
            "series/$imdbId:$season:$episode"
        }

        val subtitles = safeCall {
            request("$OPEN_SUBS_STREMIO_ENDPOINT/subtitles/$slug.json")
                .execute().fromJson<OpenSubtitleStremioDto>()
        } ?: return

        subtitles.subtitles.forEach { subtitle ->
            val subLanguage = subtitle["lang"] ?: return@forEach
            val subtitleDto = Subtitle(
                url = subtitle["url"] ?: return@forEach,
                language = "[OpenSubs] $subLanguage",
                type = SubtitleSource.ONLINE
            )

            onLinkFound(subtitleDto)
        }
    }
}