package com.flixclusive.provider.app.discord.feature.tracker

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.provider.app.discord.core.config.PrefsKey
import com.flixclusive.provider.app.discord.core.model.DiscordAuthToken
import com.flixclusive.provider.app.discord.core.presence.DiscordPresenceClient
import com.flixclusive.provider.capability.TrackerFeature
import com.flixclusive.provider.capability.TrackerProviderApi
import com.flixclusive.provider.extensions.getObject
import com.flixclusive.provider.tracker.ScrobbleAction
import com.flixclusive.provider.tracker.TrackerList

class DiscordTracker internal constructor(
    private val presence: DiscordPresenceClient,
    private val settings: DataStore<Preferences>,
) : TrackerProviderApi {
    override suspend fun getFeatures(): Set<TrackerFeature> = setOf(TrackerFeature.SCROBBLE)

    override suspend fun isAuthenticated(): Boolean =
        runCatching { settings.getObject<DiscordAuthToken>(PrefsKey.AUTH) != null }
            .getOrDefault(false)

    override suspend fun scrobble(
        action: ScrobbleAction,
        media: MediaMetadata,
        progressPercent: Float,
        atMs: Long?,
        episode: Episode?,
    ) = FlxDispatchers.withIOContext {
        when (action) {
            ScrobbleAction.START -> presence.update(media.toDiscordActivity(episode, progressPercent))
            ScrobbleAction.STOP -> presence.clear()
        }
    }

    override suspend fun getScrobbledProgress(
        item: MediaMetadata,
        episode: Episode?,
    ): Float = 0f

    override suspend fun getLists(): List<TrackerList> = unsupported()

    override suspend fun getList(id: String): TrackerList = unsupported()

    override suspend fun createList(
        name: String,
        description: String?,
    ): TrackerList = unsupported()

    override suspend fun updateList(list: TrackerList): TrackerList = unsupported()

    override suspend fun deleteList(list: TrackerList): Unit = unsupported()

    override suspend fun getListItems(
        list: TrackerList,
        page: Int,
        pageSize: Int,
    ): PaginatedMedia<MediaMetadata> = unsupported()

    override suspend fun addListItem(
        list: TrackerList,
        item: MediaMetadata,
    ): Unit = unsupported()

    override suspend fun removeListItem(
        list: TrackerList,
        item: MediaMetadata,
    ): Unit = unsupported()

    override suspend fun isInList(
        list: TrackerList,
        item: MediaMetadata,
    ): Boolean = unsupported()
}

private fun unsupported(): Nothing =
    throw UnsupportedOperationException("Discord Rich Presence does not support lists.")
