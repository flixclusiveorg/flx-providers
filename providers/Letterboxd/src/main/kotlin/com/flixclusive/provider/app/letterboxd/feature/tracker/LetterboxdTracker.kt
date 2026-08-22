package com.flixclusive.provider.app.letterboxd.feature.tracker

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.util.log.debugLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.HttpMethod
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.provider.app.letterboxd.core.config.LetterboxdConfig
import com.flixclusive.provider.app.letterboxd.core.config.PrefsKey
import com.flixclusive.provider.app.letterboxd.core.model.dto.DiaryDetailsRequest
import com.flixclusive.provider.app.letterboxd.core.model.dto.FilmRelationshipDto
import com.flixclusive.provider.app.letterboxd.core.model.dto.FilmsResponseDto
import com.flixclusive.provider.app.letterboxd.core.model.dto.ListCreationRequest
import com.flixclusive.provider.app.letterboxd.core.model.dto.ListEntriesResponseDto
import com.flixclusive.provider.app.letterboxd.core.model.dto.ListSummaryDto
import com.flixclusive.provider.app.letterboxd.core.model.dto.ListUpdateRequest
import com.flixclusive.provider.app.letterboxd.core.model.dto.ListsResponseDto
import com.flixclusive.provider.app.letterboxd.core.model.dto.LogEntriesResponseDto
import com.flixclusive.provider.app.letterboxd.core.model.dto.LogEntryCreationRequest
import com.flixclusive.provider.app.letterboxd.core.model.dto.WatchUpdateRequest
import com.flixclusive.provider.app.letterboxd.core.model.dto.WatchlistUpdateRequest
import com.flixclusive.provider.app.letterboxd.core.model.dto.toPartialMedia
import com.flixclusive.provider.app.letterboxd.core.network.CursorLedger
import com.flixclusive.provider.app.letterboxd.core.network.FilmIdResolver
import com.flixclusive.provider.app.letterboxd.core.network.LetterboxdApi
import com.flixclusive.provider.app.letterboxd.core.network.LetterboxdAuthRequiredException
import com.flixclusive.provider.app.letterboxd.core.network.LetterboxdJson
import com.flixclusive.provider.app.letterboxd.core.network.LetterboxdTokenProvider
import com.flixclusive.provider.app.letterboxd.core.network.paged
import com.flixclusive.provider.app.letterboxd.core.network.toLetterboxdCandidate
import com.flixclusive.provider.capability.TrackerFeature
import com.flixclusive.provider.capability.TrackerProviderApi
import com.flixclusive.provider.extensions.getBool
import com.flixclusive.provider.tracker.ScrobbleAction
import com.flixclusive.provider.tracker.TrackerList
import java.time.LocalDate

internal const val WATCHLIST_ID = "letterboxd_watchlist"
internal const val DIARY_ID = "letterboxd_diary"

/** Below this, a stop is treated as abandoning the film rather than finishing it. */
private const val WATCHED_THRESHOLD_PERCENT = 80f

private const val MAX_MEMBER_LISTS = 50

internal class LetterboxdTracker(
    private val api: LetterboxdApi,
    private val idResolver: FilmIdResolver,
    private val tokens: LetterboxdTokenProvider,
    private val settings: DataStore<Preferences>,
    private val providerId: String,
) : TrackerProviderApi {
    private val ledger = CursorLedger()

    override suspend fun getFeatures(): Set<TrackerFeature> =
        buildSet {
            if (settings.getBool(PrefsKey.LIST_MANAGEMENT, PrefsKey.DEFAULT_LIST_MANAGEMENT)) {
                add(TrackerFeature.LIST_MANAGEMENT)
            }

            // Both of these write to a public profile, so the feature only exists once opted in.
            if (isDiaryLoggingEnabled() || isMarkWatchedEnabled()) {
                add(TrackerFeature.SCROBBLE)
            }
        }

    override suspend fun isAuthenticated(): Boolean = tokens.isMemberAuthenticated()

    override suspend fun getLists(): List<TrackerList> {
        val member = tokens.profile() ?: throw LetterboxdAuthRequiredException()

        val synthetic =
            listOf(
                TrackerList(
                    id = WATCHLIST_ID,
                    providerId = providerId,
                    name = "Watchlist",
                    description = "Films you have saved to watch on Letterboxd.",
                    url = "https://letterboxd.com/${member.username}/watchlist/",
                ),
                TrackerList(
                    id = DIARY_ID,
                    providerId = providerId,
                    name = "Diary",
                    description = "Films you have logged on Letterboxd.",
                    url = "https://letterboxd.com/${member.username}/films/diary/",
                ),
            )

        val owned =
            runCatching {
                val body =
                    api.get(
                        path = "lists",
                        params = listOf("member" to member.id, "perPage" to MAX_MEMBER_LISTS.toString()),
                        requireMember = true,
                    )
                fromJson<ListsResponseDto>(body).items.mapNotNull { it.toTrackerList() }
            }.getOrElse {
                debugLog("Letterboxd: could not load your lists — ${it.message}")
                emptyList()
            }

        return synthetic + owned
    }

    override suspend fun getList(id: String): TrackerList =
        getLists().firstOrNull { it.id == id }
            ?: throw IllegalArgumentException("No Letterboxd list with id $id")

    override suspend fun createList(
        name: String,
        description: String?,
    ): TrackerList {
        val body = LetterboxdJson.encodeToString(ListCreationRequest(name = name, description = description))
        val response = api.send(method = HttpMethod.POST, path = "lists", body = body)

        return fromJson<ListSummaryDto>(response).toTrackerList()
            ?: throw IllegalStateException("Letterboxd did not return the created list")
    }

    override suspend fun updateList(list: TrackerList): TrackerList {
        list.requireRealList("renamed")

        val body = LetterboxdJson.encodeToString(ListUpdateRequest(name = list.name, description = list.description))
        api.send(method = HttpMethod.PATCH, path = "list/${list.id}", body = body)

        return list
    }

    override suspend fun deleteList(list: TrackerList) {
        list.requireRealList("deleted")
        api.send(method = HttpMethod.DELETE, path = "list/${list.id}")
    }

    override suspend fun getListItems(
        list: TrackerList,
        page: Int,
        pageSize: Int,
    ): PaginatedMedia<MediaMetadata> {
        val member = tokens.profile() ?: throw LetterboxdAuthRequiredException()
        val perPage = pageSize.coerceIn(1, LetterboxdConfig.MAX_PER_PAGE).toString()

        return ledger.paged(key = list.id, page = page) { cursor ->
            val params =
                buildList {
                    add("perPage" to perPage)
                    cursor?.let { add("cursor" to it) }
                }

            when (list.id) {
                WATCHLIST_ID -> {
                    val body = api.get("member/${member.id}/watchlist", params, requireMember = true)
                    val dto = fromJson<FilmsResponseDto>(body)
                    dto.next to dto.items.mapNotNull { it.toPartialMedia(providerId) }
                }

                DIARY_ID -> {
                    val body = api.get("log-entries", params + ("member" to member.id), requireMember = true)
                    val dto = fromJson<LogEntriesResponseDto>(body)
                    dto.next to dto.items.mapNotNull { it.film?.toPartialMedia(providerId) }
                }

                else -> {
                    val body = api.get("list/${list.id}/entries", params, requireMember = true)
                    val dto = fromJson<ListEntriesResponseDto>(body)
                    dto.next to dto.items.mapNotNull { it.film?.toPartialMedia(providerId) }
                }
            }
        }
    }

    override suspend fun isInList(
        list: TrackerList,
        item: MediaMetadata,
    ): Boolean {
        val filmId = item.toFilmId() ?: return false

        return when (list.id) {
            WATCHLIST_ID -> relationship(filmId)?.inWatchlist == true
            DIARY_ID -> relationship(filmId)?.watched == true
            else ->
                runCatching {
                    val body =
                        api.get(
                            path = "list/${list.id}/entries",
                            params = listOf("filmId" to filmId, "perPage" to "1"),
                            requireMember = true,
                        )
                    fromJson<ListEntriesResponseDto>(body).items.isNotEmpty()
                }.getOrDefault(false)
        }
    }

    override suspend fun addListItem(
        list: TrackerList,
        item: MediaMetadata,
    ) = setMembership(list, item, member = true)

    override suspend fun removeListItem(
        list: TrackerList,
        item: MediaMetadata,
    ) = setMembership(list, item, member = false)

    override suspend fun scrobble(
        action: ScrobbleAction,
        media: MediaMetadata,
        progressPercent: Float,
        atMs: Long?,
        episode: Episode?,
    ) {
        // Letterboxd has no now-playing concept, and no television data at all.
        if (action == ScrobbleAction.START || media.type == MediaType.SHOW) return
        if (progressPercent < WATCHED_THRESHOLD_PERCENT) return

        val markWatched = isMarkWatchedEnabled()
        val logDiary = isDiaryLoggingEnabled()
        if (!markWatched && !logDiary) return

        val filmId = media.toFilmId() ?: return

        if (markWatched) {
            setWatched(filmId, watched = true)
        }

        if (logDiary) {
            val request =
                LogEntryCreationRequest(
                    filmId = filmId,
                    diaryDetails = DiaryDetailsRequest(diaryDate = LocalDate.now().toString()),
                )
            api.send(
                method = HttpMethod.POST,
                path = "log-entries",
                body = LetterboxdJson.encodeToString(request),
            )
        }
    }

    /** Letterboxd stores no playback position, only whether a film was watched. */
    override suspend fun getScrobbledProgress(
        item: MediaMetadata,
        episode: Episode?,
    ): Float = 0f

    private suspend fun setMembership(
        list: TrackerList,
        item: MediaMetadata,
        member: Boolean,
    ) {
        val filmId =
            item.toFilmId()
                ?: throw IllegalArgumentException("\"${item.title}\" has no Letterboxd, TMDB or IMDB id to match on.")

        when (list.id) {
            WATCHLIST_ID -> {
                val body = LetterboxdJson.encodeToString(WatchlistUpdateRequest(watchlisted = member))
                api.send(method = HttpMethod.PATCH, path = "me/watchlist/$filmId", body = body)
            }

            DIARY_ID -> setWatched(filmId, watched = member)

            // The ListUpdateRequest entry schema is not published, so guessing a body here
            // would fail at runtime rather than at build time.
            else -> throw UnsupportedOperationException(
                "Letterboxd lists are read-only here — add or remove films in the Watchlist or Diary.",
            )
        }

        ledger.forget(list.id)
    }

    private suspend fun setWatched(
        filmId: String,
        watched: Boolean,
    ) {
        val body = LetterboxdJson.encodeToString(WatchUpdateRequest(watched = watched))
        api.send(method = HttpMethod.PATCH, path = "me/watch/$filmId", body = body)
    }

    private suspend fun relationship(filmId: String): FilmRelationshipDto? =
        runCatching {
            val body = api.get("${LetterboxdConfig.FILM_PATH}/$filmId/me", requireMember = true)
            fromJson<FilmRelationshipDto>(body).effective
        }.getOrNull()

    private suspend fun isDiaryLoggingEnabled(): Boolean =
        settings.getBool(PrefsKey.DIARY_LOGGING, PrefsKey.DEFAULT_DIARY_LOGGING)

    private suspend fun isMarkWatchedEnabled(): Boolean =
        settings.getBool(PrefsKey.MARK_WATCHED, PrefsKey.DEFAULT_MARK_WATCHED)

    private fun ListSummaryDto.toTrackerList(): TrackerList? {
        if (id.isBlank() || name.isBlank()) return null

        return TrackerList(
            id = id,
            providerId = providerId,
            name = name,
            description = description,
            itemCount = filmCount,
            url = links.firstOrNull { it.type.equals("letterboxd", ignoreCase = true) }?.url,
        )
    }

    private fun TrackerList.requireRealList(verb: String) {
        if (id == WATCHLIST_ID || id == DIARY_ID) {
            throw UnsupportedOperationException("Your Letterboxd $name cannot be $verb.")
        }
    }

    /**
     * Path-addressed calls need a real Letterboxd id: `/film/tmdb:949` resolves, but
     * `/film/imdb:tt…` 404s. [FilmIdResolver] normalises both, and caches the answer.
     */
    private suspend fun MediaMetadata.toFilmId(): String? {
        val candidate = toLetterboxdCandidate(this@LetterboxdTracker.providerId) ?: return null
        return runCatching { idResolver.toLetterboxdId(candidate) }.getOrNull()
    }
}
