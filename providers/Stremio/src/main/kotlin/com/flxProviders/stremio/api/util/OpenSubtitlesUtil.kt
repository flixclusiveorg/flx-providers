package com.flxProviders.stremio.api.util

import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.provider.SubtitleSource
import okhttp3.OkHttpClient

internal object OpenSubtitlesUtil {
    private const val OPEN_SUBS_STREMIO_ENDPOINT = "https://opensubtitles-v3.strem.io"

    private data class OpenSubtitleStremioDto(
        val subtitles: List<Map<String, String>>,
    )

    fun OkHttpClient.fetchSubtitles(
        imdbId: String,
        season: Int? = null,
        episode: Int? = null
    ): List<Subtitle> {
        if (!imdbId.startsWith("tt"))
            return emptyList()

        val slug = if(season == null) {
            "movie/$imdbId"
        } else {
            "series/$imdbId:$season:$episode"
        }

        val subtitles = safeCall {
            request("$OPEN_SUBS_STREMIO_ENDPOINT/subtitles/$slug.json")
                .execute().fromJson<OpenSubtitleStremioDto>()
        } ?: return emptyList()

        val links = mutableListOf<Subtitle>()
        subtitles.subtitles.forEach { subtitle ->
            val subtitleDto = Subtitle(
                url = subtitle["url"] ?: return@forEach,
                language = subtitle["lang"] ?: return@forEach,
                type = SubtitleSource.ONLINE
            )

            links.add(subtitleDto)
        }

        return links
    }
}