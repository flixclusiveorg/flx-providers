package com.flixclusive.provider.app.trakt.core.network

import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.provider.app.trakt.core.config.TraktApiConfig
import com.flixclusive.provider.app.trakt.core.model.TraktList
import com.flixclusive.provider.app.trakt.core.model.TraktMedia
import com.flixclusive.provider.app.trakt.core.model.TraktSmartList
import com.flixclusive.provider.app.trakt.core.model.TraktUserWrapper
import com.flixclusive.provider.app.trakt.core.model.TraktWatchNowSource
import com.flixclusive.provider.app.trakt.core.network.dto.request.ListCreateRequest
import com.flixclusive.provider.app.trakt.core.network.dto.request.ListItemActionRequest
import com.flixclusive.provider.app.trakt.core.network.dto.request.ScrobbleRequest
import com.flixclusive.provider.app.trakt.core.network.dto.request.TraktSearchRequestV2
import com.flixclusive.provider.app.trakt.core.network.dto.response.MinimalWatchedItemMap
import com.flixclusive.provider.app.trakt.core.network.dto.response.MinimalWatchlist
import com.flixclusive.provider.app.trakt.core.network.dto.response.ScrobblePlaybackResponse
import com.flixclusive.provider.app.trakt.core.network.dto.response.TraktEpisodeResponse
import com.flixclusive.provider.app.trakt.core.network.dto.response.TraktEpisodeResponse.Companion.toEpisode
import com.flixclusive.provider.app.trakt.core.network.dto.response.TraktGenericMediaItemResponse
import com.flixclusive.provider.app.trakt.core.network.dto.response.TraktLikedList
import com.flixclusive.provider.app.trakt.core.network.dto.response.TraktMediaWatchNowSources
import com.flixclusive.provider.app.trakt.core.network.dto.response.TraktSearchMediaItemResponse
import com.flixclusive.provider.app.trakt.core.network.dto.response.TraktSearchResponseV2
import com.flixclusive.provider.app.trakt.core.network.dto.response.TraktSeasonResponse
import com.flixclusive.provider.app.trakt.core.network.dto.response.TraktSeasonResponse.Companion.toSeason
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

internal interface TraktApiService {
    @GET("users/settings")
    suspend fun getUser(): TraktUserWrapper

    @POST("users/me/lists")
    suspend fun addList(@Body request: ListCreateRequest): TraktList

    @PUT("users/me/lists/{id}")
    suspend fun updateList(
        @Path("id") id: String,
        @Body request: ListCreateRequest
    ): TraktList

    @DELETE("users/me/lists/{id}")
    suspend fun removeList(
        @Path("id") id: String
    )

    @GET("users/me/lists")
    suspend fun getLists(
        @Query("extended") extended: String = "images"
    ): List<TraktList>

    @GET("users/me/lists/{id}")
    suspend fun getList(
        @Path("id") id: String,
        @Query("extended") extended: String = "images"
    ): TraktList

    @POST("users/me/lists/{id}/items")
    suspend fun addListItems(
        @Path("id") id: String,
        @Body request: ListItemActionRequest
    )

    @POST("sync/{type}")
    suspend fun addTypedItems(
        @Path("type") type: String,
        @Body request: ListItemActionRequest
    )

    @POST("users/me/lists/{id}/items/remove")
    suspend fun removeListItems(
        @Path("id") id: String,
        @Body request: ListItemActionRequest
    )

    @POST("sync/{type}/remove")
    suspend fun removeTypedItems(
        @Path("type") type: String,
        @Body request: ListItemActionRequest
    )

    @GET("users/me/lists/{id}/items/{type}")
    suspend fun getListItems(
        @Path("id") id: String,
        @Path("type") type: String = "movie,show",
        @Query("extended") extended: String = "full,images",
        @Query("page") page: Int,
        @Query("limit") limit: Int = TraktApiConfig.PAGE_RESULTS_LIMIT
    ): List<TraktGenericMediaItemResponse>

    @GET("users/me/{type}/{mediaType}")
    suspend fun getTypedItems(
        @Path("type") type: String,
        @Path("mediaType") mediaType: String = "movie,show",
        @Query("page") page: Int,
        @Query("limit") limit: Int = TraktApiConfig.PAGE_RESULTS_LIMIT,
        @Query("extended") extended: String = "full,images",
        @Query("sort_by") sortBy: String = "added",
        @Query("sort_how") sortHow: String = "desc",
        @Query("ignore_watched") ignoreWatched: Boolean = false,
    ): List<TraktGenericMediaItemResponse>

    @GET("users/me/watched/{type}")
    suspend fun getMinimalWatched(
        @Path("type") type: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int = TraktApiConfig.PAGE_RESULTS_LIMIT,
        @Query("extended") extended: String = "min",
        @Query("specials") includeSpecials: Boolean = true,
        @Query("season_numbers") includeSeasonNumbers: Boolean = true,
    ) : MinimalWatchedItemMap

    @GET("v3/users/me/watchlist/minimal")
    suspend fun getMinimalWatchlist(): MinimalWatchlist

    @GET("v3/{type}/{id}/me/lists")
    suspend fun getListsContainingItem(
        @Path("type") type: String,
        @Path("id") filmId: String,
    ): List<Int>

    @GET("search/{id_type}/{id}")
    suspend fun searchById(
        @Path("id_type") idType: String,
        @Path("id") id: String,
        @Query("extended") extended: String = "full,images"
    ): List<TraktSearchMediaItemResponse>

    @GET("{type}/{id}")
    suspend fun getMetadata(
        @Path("type") type: String,
        @Path("id") id: String,
        @Query("extended") extended: String = "full,images"
    ): TraktMedia

    @GET("shows/{id}/seasons")
    suspend fun getSeasons(
        @Path("id") showId: String,
        @Query("extended") extended: String = "full,images"
    ): List<TraktSeasonResponse>

    @GET("shows/{id}/seasons/{season}")
    suspend fun getEpisodes(
        @Path("id") showId: String,
        @Path("season") season: Int,
        @Query("extended") extended: String = "full,images"
    ): List<TraktEpisodeResponse>

    @GET("{type}/{id}/related")
    suspend fun getRelatedItems(
        @Path("type") type: String,
        @Path("id") id: String,
        @Query("extended") extended: String = "full,images",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = TraktApiConfig.PAGE_RESULTS_LIMIT,
    ): List<TraktMedia>

    @GET("users/saved_filters/{type}")
    suspend fun getSavedFilters(
        @Path("type") type: String,
    ): List<TraktSmartList>

    @POST("scrobble/{action}")
    suspend fun scrobble(
        @Path("action") action: String,
        @Body request: ScrobbleRequest,
    )

    @GET("sync/playback/{type}")
    suspend fun getPlaybackProgress(
        @Path("type") type: String,
        @Query("start_at") startAt: String? = null,
        @Query("end_at") endAt: String? = null,
    ): List<ScrobblePlaybackResponse>

    @GET("users/likes/lists")
    suspend fun getLikedLists(
        @Query("page") page: Int,
        @Query("limit") limit: Int = TraktApiConfig.PAGE_RESULTS_LIMIT,
        @Query("extended") extended: String = "full",
    ): List<TraktLikedList>

    @GET("watchnow/sources/{country}")
    suspend fun getWatchNowSources(
        @Path("country") country: String,
    ): List<Map<String, List<TraktWatchNowSource>>>

    @GET("{type}/{id}/watchnow/{country}")
    suspend fun getStreams(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("country") country: String,
        @Query("links") linksToInclude: String = "direct,android",
    ): Map<String, TraktMediaWatchNowSources>

    @GET("shows/{id}/seasons/{season}/episodes/{episode}/watchnow/{country}")
    suspend fun getEpisodeStreams(
        @Path("id") showId: String,
        @Path("season") season: Int,
        @Path("episode") episode: Int,
        @Path("country") country: String,
        @Query("links") linksToInclude: String = "direct,android",
    ): Map<String, TraktMediaWatchNowSources>

    @GET
    suspend fun getCatalogItems(
        @Url url: String,
    ): List<TraktGenericMediaItemResponse>

    @GET
    suspend fun getPopularCatalogItems(
        @Url url: String,
    ): List<TraktMedia>

    @POST
    @Headers("Content-Type: application/json")
    suspend fun search(
        @Header("X-TYPESENSE-API-KEY") apiKey: String,
        @Body requestBody: TraktSearchRequestV2,
        @Url url: String,
    ): TraktSearchResponseV2

    companion object {
        suspend fun TraktApiService.getFullSeasons(showId: String): List<Season> {
            try {
                val traktSeasons = getSeasons(showId)
                return traktSeasons
                    .chunked(10) // avoid making too many concurrent requests for shows with many seasons
                    .flatMap { seasonChunk ->
                        seasonChunk.map { season ->
                            coroutineScope {
                                async {
                                    val traktEpisodes = getEpisodes(showId, season.number)
                                    val episodes = traktEpisodes.map { episode -> episode.toEpisode() }
                                    season.toSeason(episodes)
                                }
                            }
                        }.awaitAll()
                    }
            } catch (e: Throwable) {
                throw RuntimeException("Failed to fetch seasons for show with id $showId", e)
            }
        }

        fun create(client: OkHttpClient): TraktApiService {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            }

            return Retrofit.Builder()
                .baseUrl(TraktApiConfig.API_BASE_URL)
                .addConverterFactory(json.asConverterFactory(
                    "application/json; charset=utf-8".toMediaType()))
                .client(client)
                .build()
                .create(TraktApiService::class.java)
        }
    }
}