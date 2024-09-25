package com.flxProviders.sudoflix.api.nsbx

import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.film.FilmDetails
import com.flixclusive.provider.Provider
import okhttp3.OkHttpClient

internal class VidBingeApi(
    client: OkHttpClient,
    provider: Provider
) : AbstractNsbxApi(
    client = client,
    provider = provider
) {
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