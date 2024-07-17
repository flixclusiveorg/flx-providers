package com.flxProviders.sudoflix.api.nsbx

import android.net.Uri
import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.provider.SubtitleSource
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.provider.ProviderApi
import com.flxProviders.sudoflix.api.nsbx.dto.NsbxProviders
import com.flxProviders.sudoflix.api.nsbx.dto.NsbxSource
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient

internal class VidBingeApi(
    client: OkHttpClient
) : AbstractNsbxApi(client) {
    override val baseUrl = "https://api.whvx.net"
    override val name = "VidBinge"
    override val streamSourceUrl = "$baseUrl/source"

    override fun FilmDetails.getQuery(
        season: Int?,
        episode: Int?
    ): String {
        val filmType = if (season != null) "show" else FilmType.MOVIE.type

        var query = """
            {"title":"$title","releaseYear":${year},"tmdbId":"$tmdbId","imdbId":"$imdbId","type":"$filmType"
        """.trimIndent()

        if (season != null) {
            query += """
                ,"season":"$season","episode":"$episode"
            """.trimIndent()
        }

        return "$query}"
    }
}