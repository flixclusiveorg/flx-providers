package com.flxProviders.sudoflix.api.nsbx

import android.net.Uri
import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.model.provider.link.SubtitleSource
import com.flixclusive.model.film.FilmDetails
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import com.flxProviders.sudoflix.api.nsbx.dto.NsbxProviders
import com.flxProviders.sudoflix.api.nsbx.dto.NsbxSource
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient

internal abstract class AbstractNsbxApi(
    client: OkHttpClient,
    provider: Provider,
) : ProviderApi(
    client = client,
    provider = provider
) {
    abstract val streamSourceUrl: String
    abstract val name: String
    private val origin = "https://sudo-flix.lol"
    private val headers = mapOf("Origin" to origin).toHeaders()

    override suspend fun getLinks(
        watchId: String,
        film: FilmDetails,
        episode: Episode?,
        onLinkFound: (MediaLink) -> Unit
    ) {
        val availableProviders = getAvailableProviders()

        if (availableProviders.isEmpty())
            throw IllegalStateException("No available providers for $name")

        for (i in availableProviders.indices) {
            try {
                val provider = availableProviders[i]
                val query = film.getQuery(
                    season = episode?.season,
                    episode = episode?.number
                )

                val searchRawResponse = client.request(
                    url = "$baseUrl/search?provider=$provider&query=${Uri.encode(query)}",
                    headers = headers,
                ).execute()

                val searchRawString = searchRawResponse.body?.string()
                    ?: throw IllegalStateException("[$name]> Could not search for film")

                val searchResponse = fromJson<Map<String, String>>(searchRawString)
                val encryptedSourceId = searchResponse["url"]
                    ?: throw IllegalStateException("[$name]> Could not get encrypted source id")

                val source = client.request(
                    url = "$streamSourceUrl?resourceId=${Uri.encode(encryptedSourceId)}&provider=$provider",
                    headers = headers,
                ).execute()
                    .use {
                        val stringResponse = it.body?.string()

                        if (!it.isSuccessful || stringResponse == null || stringResponse.contains("\"error\"")) {
                            errorLog(stringResponse ?: "Unknown $name Error")
                            throw IllegalStateException("[$name]> Could not get source link")
                        }

                        fromJson<NsbxSource>(stringResponse)
                    }

                val streamFlags = setOf(
                    Flag.RequiresAuth(customHeaders = headers.toMap())
                )

                asyncCalls(
                    {
                        source.stream.mapAsync {
                            it.qualities?.entries?.mapAsync { (serverName, qualitySource) ->
                                onLinkFound(
                                    Stream(
                                        name = serverName,
                                        url = qualitySource.url,
                                        flags = streamFlags
                                    )
                                )
                            }
                        }
                    },
                    {
                        source.stream.mapAsync {
                            if (it.playlist == null) {
                                return@mapAsync
                            }

                            onLinkFound(
                                Stream(
                                    name = name,
                                    url = it.playlist,
                                    flags = streamFlags
                                )
                            )
                        }
                    },
                    {
                        source.stream.mapAsync {
                            it.captions.mapAsync { caption ->
                                onLinkFound(
                                    Subtitle(
                                        url = caption.url,
                                        language = "[$name] ${caption.language}",
                                        type = SubtitleSource.ONLINE
                                    )
                                )
                            }
                        }
                    },
                )
            }
            catch (e: Exception) {
                if (i != availableProviders.indices.last) {
                    errorLog(e.stackTraceToString())
                    continue
                }

                throw e
            }
        }
    }

    private fun getAvailableProviders(): List<String> {
        val response = client.request(
            url = "$baseUrl/status",
            headers = headers
        ).execute().body?.string()
            ?: throw NullPointerException("[$name]> Could not get available providers")

        if (response.contains("\"error\""))
            throw IllegalStateException("[$name]> Internal server error")

        return fromJson<NsbxProviders>(response).providers
    }

    protected open fun FilmDetails.getQuery(
        season: Int?,
        episode: Int?
    ): String {
        val filmType = if (season != null) "show" else FilmType.MOVIE.type

        return """
            {"title":"$title","releaseYear":${year},"tmdbId":"$tmdbId","imdbId":"$imdbId","type":"$filmType","season":"${season ?: ""}","episode":"${episode ?: ""}"}
        """.trimIndent()
    }
}