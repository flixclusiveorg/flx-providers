package com.flixclusive.provider.app.trakt.feature.tracker

import android.content.Context
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.util.android.showToast
import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.provider.extensions.getBool
import com.flixclusive.provider.extensions.getString
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.provider.ProviderPlugin
import com.flixclusive.provider.capability.TrackerFeature
import com.flixclusive.provider.capability.TrackerProviderApi
import com.flixclusive.provider.tracker.ScrobbleAction
import com.flixclusive.provider.tracker.TrackerList
import com.flixclusive.provider.app.trakt.core.config.PrefsKey
import com.flixclusive.provider.app.trakt.core.config.TraktApiConfig
import com.flixclusive.provider.app.trakt.core.network.TraktApiService
import com.flixclusive.provider.app.trakt.core.network.dto.request.ListCreateRequest
import com.flixclusive.provider.app.trakt.core.network.dto.request.ListItemActionRequest.Companion.toListItemActionRequest
import com.flixclusive.provider.app.trakt.core.network.dto.request.ScrobbleEpisode.Companion.toScrobbleEpisode
import com.flixclusive.provider.app.trakt.core.network.dto.request.ScrobbleMedia.Companion.toScrobbleMedia
import com.flixclusive.provider.app.trakt.core.network.dto.request.ScrobbleRequest
import com.flixclusive.provider.app.trakt.core.network.dto.response.MinimalWatchedItemMap
import com.flixclusive.provider.app.trakt.core.network.dto.response.TraktGenericMediaItemResponse
import com.flixclusive.provider.app.trakt.core.network.util.OkHttpClientUtil

class TraktTracker internal constructor(
    private val context: Context,
    private val plugin: ProviderPlugin,
    private val settings: DataStore<Preferences>
) : TrackerProviderApi {
    companion object {
        private const val ERROR_DISALLOWED_LIST_MANAGEMENT = "List management is disabled in your Trakt settings"
        private const val ERROR_DISALLOWED_SCROBBLE = "Scrobbling is disabled in your Trakt settings"

        private const val TRAKT_WATCHLIST_ID = "trakt_flixclusive_watchlist"
        private const val TRAKT_WATCHED_ID = "trakt_flixclusive_watched_history"
    }

    private val apiService by lazy {
        TraktApiService.create(
            OkHttpClientUtil.createNonCachedClient(settings)
        )
    }

    private val cachedApiService by lazy {
        TraktApiService.create(
            OkHttpClientUtil.createCachedClient(
                context = context,
                settings = settings,
                cacheMaxAge = 60
            )
        )
    }

    private suspend fun canPerformAction(key: String) {
        val userId = settings.getString(PrefsKey.PREFS_AUTH_USER_ID, null) ?: return

        val userSpecificKey = PrefsKey.getPrefKeyForUser(userId, key)
        val isAllowed = settings.getBool(userSpecificKey, true)
        if (!isAllowed) {
            FlxDispatchers.withMainContext {
                context.showToast("Action not allowed: $key is disabled in settings.")
            }

            error(
                when (key) {
                    PrefsKey.PREFS_LIST_MANAGEMENT -> ERROR_DISALLOWED_LIST_MANAGEMENT
                    PrefsKey.PREFS_SCROBBLE -> ERROR_DISALLOWED_SCROBBLE
                    else -> "Action not allowed. It is disabled in settings."
                }
            )
        }
    }

    override suspend fun getFeatures(): Set<TrackerFeature> {
        val enabledFeatures = mutableSetOf<TrackerFeature>()
        val userId = settings.getString(PrefsKey.PREFS_AUTH_USER_ID, null)
            ?: return setOf(
                TrackerFeature.LIST_MANAGEMENT,
                TrackerFeature.SCROBBLE
            )

        val isListManagementAllowed = settings.getBool(
            PrefsKey.getPrefKeyForUser(
                userId = userId,
                baseKey = PrefsKey.PREFS_LIST_MANAGEMENT
            ),
            true
        )

        val isScrobblingAllowed = settings.getBool(
            PrefsKey.getPrefKeyForUser(
                userId = userId,
                baseKey = PrefsKey.PREFS_SCROBBLE
            ),
            true
        )

        if (isListManagementAllowed) {
            enabledFeatures.add(TrackerFeature.LIST_MANAGEMENT)
        }

        if (isScrobblingAllowed) {
            enabledFeatures.add(TrackerFeature.SCROBBLE)
        }

        return enabledFeatures.toSet()
    }

    override suspend fun isInList(
        list: TrackerList,
        item: MediaMetadata
    ): Boolean {
        canPerformAction(PrefsKey.PREFS_LIST_MANAGEMENT)

        return FlxDispatchers.withIOContext {
            when (list.id) {
                TRAKT_WATCHLIST_ID -> {
                    val traktId = item.externalIds[MediaIdSource.TRAKT]
                        ?.toIntOrNull() ?: return@withIOContext false

                    val watchlistItems = apiService.getMinimalWatchlist()
                    watchlistItems.isInList(traktId)
                }
                TRAKT_WATCHED_ID -> {
                    val traktId = item.externalIds[MediaIdSource.TRAKT] ?: return@withIOContext false
                    val type = when (item.type) {
                        MediaType.MOVIE -> "movies"
                        MediaType.SHOW -> "shows"
                    }

                    var page = 1
                    var watchedItems: MinimalWatchedItemMap? = null

                    do {
                        if (watchedItems?.isWatched(traktId) == true) {
                            return@withIOContext true
                        }

                        watchedItems = cachedApiService.getMinimalWatched(
                            page = page++, type = type
                        )
                    } while (watchedItems.isNotEmpty())

                    false
                }
                else -> cachedApiService.getListsContainingItem(
                    filmId = item.id,
                    type = when (item.type) {
                        MediaType.MOVIE -> "movies"
                        MediaType.SHOW -> "shows"
                    }
                ).fastAny { it.toString() == list.id }
            }
        }
    }

    override suspend fun getList(id: String): TrackerList {
        canPerformAction(PrefsKey.PREFS_LIST_MANAGEMENT)

        return FlxDispatchers.withIOContext {
            when (id) {
                TRAKT_WATCHLIST_ID -> getWatchlist()
                TRAKT_WATCHED_ID -> getWatchedList()
                else -> cachedApiService.getList(id).toTrackerList(plugin.id)
            }
        }
    }

    override suspend fun addListItem(
        list: TrackerList,
        item: MediaMetadata
    ) {
        canPerformAction(PrefsKey.PREFS_LIST_MANAGEMENT)

        FlxDispatchers.withIOContext {
            try {
                when (list.id) {
                    TRAKT_WATCHLIST_ID -> {
                        apiService.addTypedItems(
                            type = "watchlist",
                            request = item.toListItemActionRequest()
                        )
                    }
                    TRAKT_WATCHED_ID -> {
                        apiService.addTypedItems(
                            type = "history",
                            request = item.toListItemActionRequest()
                        )
                    }
                    else -> {
                        apiService.addListItems(
                            id = list.id,
                            request = item.toListItemActionRequest()
                        )
                    }
                }
            } catch (e: Throwable) {
                errorLog("Failed to add item to list: ${e.message}")
                e.printStackTrace()

                FlxDispatchers.withMainContext {
                    context.showToast("Failed to add item to list: ${e.message}")
                }
            }
        }
    }

    override suspend fun createList(
        name: String,
        description: String?
    ): TrackerList {
        canPerformAction(PrefsKey.PREFS_LIST_MANAGEMENT)

        return FlxDispatchers.withIOContext {
            val traktList = apiService.addList(
                request = ListCreateRequest(
                    name = name,
                    description = description ?: "",
                )
            )

            traktList.toTrackerList(plugin.id)
        }
    }

    override suspend fun deleteList(list: TrackerList) {
        canPerformAction(PrefsKey.PREFS_LIST_MANAGEMENT)

        return FlxDispatchers.withIOContext {
            if (list.id == TRAKT_WATCHLIST_ID || list.id == TRAKT_WATCHED_ID) {
                error("Cannot delete default lists")
            } else {
                apiService.removeList(id = list.id)
            }
        }
    }

    override suspend fun getListItems(
        list: TrackerList,
        page: Int,
        pageSize: Int,
    ): PaginatedMedia<MediaMetadata> {
        canPerformAction(PrefsKey.PREFS_LIST_MANAGEMENT)

        return FlxDispatchers.withIOContext {
            val listDetails = getList(id = list.id)
            val totalPages = listDetails.itemCount?.div(TraktApiConfig.PAGE_RESULTS_LIMIT) ?: 0
            val items = when (list.id) {
                TRAKT_WATCHLIST_ID -> cachedApiService.getTypedItems(
                    page = page,
                    limit = TraktApiConfig.PAGE_RESULTS_LIMIT,
                    type = "watchlist",
                    ignoreWatched = true
                )
                TRAKT_WATCHED_ID -> {
                    getWatchedItems(page, pageSize)
                }
                else -> cachedApiService.getListItems(
                    id = list.id,
                    page = page,
                    limit = pageSize,
                )
            }

            PaginatedMedia(
                page = page,
                totalPages = totalPages,
                hasNextPage = items.size >= pageSize,
                results = items.fastMap {
                    it.toPartialMedia(providerId = plugin.id)
                }
            )
        }
    }

    override suspend fun getLists(): List<TrackerList> {
        canPerformAction(PrefsKey.PREFS_LIST_MANAGEMENT)

        return FlxDispatchers.withIOContext {
            val userLists = cachedApiService.getLists().fastMap {
                it.toTrackerList(plugin.id)
            }

            buildList {
                addAll(userLists)
                add(getWatchlist())
                add(getWatchedList())
            }
        }
    }

    override suspend fun isAuthenticated(): Boolean {
        return settings.getString(PrefsKey.PREFS_AUTH_USER_ID, null) != null
    }

    override suspend fun removeListItem(
        list: TrackerList,
        item: MediaMetadata
    ) {
        canPerformAction(PrefsKey.PREFS_LIST_MANAGEMENT)

        FlxDispatchers.withIOContext {
            try {
                when (list.id) {
                    TRAKT_WATCHLIST_ID -> {
                        apiService.removeTypedItems(
                            type = "watchlist",
                            request = item.toListItemActionRequest()
                        )
                    }
                    TRAKT_WATCHED_ID -> {
                        apiService.removeTypedItems(
                            type = "history",
                            request = item.toListItemActionRequest()
                        )
                    }
                    else -> {
                        apiService.removeListItems(
                            id = list.id,
                            request = item.toListItemActionRequest()
                        )
                    }
                }
            } catch (e: Throwable) {
                errorLog("Failed to remove item from list: ${e.message}")
                e.printStackTrace()

                FlxDispatchers.withMainContext {
                    context.showToast("Failed to remove item from list: ${e.message}")
                }
            }
        }
    }

    override suspend fun updateList(
        list: TrackerList,
    ): TrackerList {
        canPerformAction(PrefsKey.PREFS_LIST_MANAGEMENT)

        return FlxDispatchers.withIOContext {
            if (list.id == TRAKT_WATCHLIST_ID || list.id == TRAKT_WATCHED_ID) {
                error("Cannot update default lists")
            }

            val updatedList = apiService.updateList(
                id = list.id,
                request = ListCreateRequest(
                    name = list.name,
                    description = list.description ?: "",
                )
            )

            updatedList.toTrackerList(plugin.id)
        }
    }

    override suspend fun scrobble(
        action: ScrobbleAction,
        media: MediaMetadata,
        progressPercent: Float,
        atMs: Long?,
        episode: Episode?
    ) {
        canPerformAction(PrefsKey.PREFS_SCROBBLE)

        FlxDispatchers.withIOContext {
            val request = when (media.type) {
                MediaType.MOVIE -> {
                    ScrobbleRequest(
                        movie = media.toScrobbleMedia(),
                        progress = progressPercent,
                    )
                }
                MediaType.SHOW -> {
                    requireNotNull(episode) {
                        "Episode information is required for scrobbling shows"
                    }

                    ScrobbleRequest(
                        show = media.toScrobbleMedia(),
                        episode = episode.toScrobbleEpisode(),
                        progress = progressPercent,
                    )
                }
            }

            apiService.scrobble(
                action = action.toRequest(),
                request = request
            )
        }
    }

    override suspend fun getScrobbledProgress(
        item: MediaMetadata,
        episode: Episode?
    ): Float {
        canPerformAction(PrefsKey.PREFS_SCROBBLE)

        require(item.providerId == plugin.id) {
            "Item does not belong to this provider"
        }

        return FlxDispatchers.withIOContext {
            val progressList = cachedApiService.getPlaybackProgress(
                type = when (item.type) {
                    MediaType.MOVIE -> "movies"
                    MediaType.SHOW -> "episodes"
                },
            )

            val progressItem = progressList.fastFirstOrNull {
                val isSameMedia = it.media.ids.asMap().values.any { id ->
                    item.externalIds.containsValue(id)
                }

                val isSameEpisode = when (item.type) {
                    MediaType.MOVIE -> true
                    MediaType.SHOW -> {
                        val scrobbleEpisode = it.episode ?: return@fastFirstOrNull false
                        scrobbleEpisode.season == episode?.season &&
                                scrobbleEpisode.number == episode.number
                    }
                }

                isSameMedia && isSameEpisode
            }

            progressItem?.progress ?: 0f
        }
    }

    private fun ScrobbleAction.toRequest(): String {
        return when (this) {
            ScrobbleAction.START -> "start"
            ScrobbleAction.STOP -> "stop"
        }
    }

    private suspend fun getWatchedItems(
        page: Int,
        pageSize: Int
    ): List<TraktGenericMediaItemResponse> {
        val movies = cachedApiService.getTypedItems(
            page = page,
            limit = pageSize / 2,
            type = "watched",
            mediaType = "movies"
        )

        val shows = cachedApiService.getTypedItems(
            page = page,
            limit = pageSize - movies.size,
            type = "watched",
            mediaType = "shows"
        )

        return (movies + shows).sortedBy { it.listedAtAsLong ?: 0L }
    }

    private suspend fun getWatchlist(): TrackerList {
        val items = cachedApiService.getTypedItems(
            page = 1,
            limit = 3,
            type = "watchlist",
            extended = "images",
            ignoreWatched = true
        )

        val previews = items.take(3).fastMapNotNull { it.media.poster ?: it.media.backdrop }
        val lastUpdatedAt = items.firstOrNull()?.listedAtAsLong ?: (System.currentTimeMillis() - 1)

        return TrackerList(
            id = TRAKT_WATCHLIST_ID,
            providerId = plugin.id,
            name = "Trakt Watchlist",
            description = "A list of movies and shows you want to watch, synced with your Trakt account.",
            url = "https://trakt.tv/users/me/watchlist",
            updatedAt = lastUpdatedAt,
            images = previews
        )
    }

    private suspend fun getWatchedList(): TrackerList {
        val items = cachedApiService.getTypedItems(
            page = 1,
            limit = 3,
            type = "watched",
            mediaType = "movies",
            extended = "images"
        ) + cachedApiService.getTypedItems(
            page = 1,
            limit = 3,
            type = "watched",
            mediaType = "shows",
            extended = "images"
        )

        val previews = items
            .sortedByDescending { it.listedAtAsLong ?: 0L }
            .take(3)
            .fastMapNotNull { it.media.poster ?: it.media.backdrop }
        val lastUpdatedAt = items.maxByOrNull { it.listedAtAsLong ?: 0L }
            ?.listedAtAsLong
            ?: (System.currentTimeMillis() - 1)

        return TrackerList(
            id = TRAKT_WATCHED_ID,
            providerId = plugin.id,
            name = "Trakt Watched",
            description = "A list of movies and shows you've watched, synced with your Trakt account.",
            url = "https://trakt.tv/users/me/watched",
            updatedAt = lastUpdatedAt,
            images = previews
        )
    }
}