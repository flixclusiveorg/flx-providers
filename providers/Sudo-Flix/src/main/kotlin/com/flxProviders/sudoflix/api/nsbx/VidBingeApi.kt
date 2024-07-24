package com.flxProviders.sudoflix.api.nsbx

import com.flixclusive.core.util.film.FilmType
import com.flixclusive.model.tmdb.FilmDetails
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