package com.flixclusive.provider.app.letterboxd

import android.content.Context
import androidx.compose.runtime.Composable
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.provider.FlixclusiveProvider
import com.flixclusive.provider.ProviderPlugin
import com.flixclusive.provider.app.letterboxd.core.model.dto.MemberAccountDto
import com.flixclusive.provider.app.letterboxd.core.model.dto.toMember
import com.flixclusive.provider.app.letterboxd.core.network.LetterboxdApi
import com.flixclusive.provider.app.letterboxd.core.network.LetterboxdAuthService
import com.flixclusive.provider.app.letterboxd.core.network.FilmIdResolver
import com.flixclusive.provider.app.letterboxd.core.network.LetterboxdClient
import com.flixclusive.provider.app.letterboxd.core.network.LetterboxdTokenProvider
import com.flixclusive.provider.app.letterboxd.feature.catalog.LetterboxdCatalogProvider
import com.flixclusive.provider.app.letterboxd.feature.crossmatch.LetterboxdCrossMatcher
import com.flixclusive.provider.app.letterboxd.feature.metadata.LetterboxdMetadataProvider
import com.flixclusive.provider.app.letterboxd.feature.search.LetterboxdSearchProvider
import com.flixclusive.provider.app.letterboxd.feature.settings.LetterboxdSettingsScreen
import com.flixclusive.provider.app.letterboxd.feature.tracker.LetterboxdTracker
import com.flixclusive.provider.capability.CatalogProviderApi
import com.flixclusive.provider.capability.CrossMatchProviderApi
import com.flixclusive.provider.capability.MediaMetadataProviderApi
import com.flixclusive.provider.capability.SearchProviderApi
import com.flixclusive.provider.capability.TrackerProviderApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient

/**
 * Letterboxd is a films-only metadata and social source. It serves no playable media,
 * so [getMediaLinkApi] is deliberately left unimplemented.
 */
@FlixclusiveProvider
class LetterboxdPlugin : ProviderPlugin() {
    // `manifest` and `settings` are injected after construction, so every derived value
    // is deferred until first use.
    private val writeClient: OkHttpClient by lazy { LetterboxdClient.createVanillaClient() }
    private val authService by lazy { LetterboxdAuthService(writeClient) }
    private val tokens by lazy { LetterboxdTokenProvider(settings, authService) }

    /** Uncached transport for `/me` and the token exchange, which need no Context. */
    private val authApi by lazy { LetterboxdApi(writeClient, writeClient, tokens) }

    private val componentsMutex = Mutex()
    private var components: Components? = null

    private class Components(
        val readClient: OkHttpClient,
        val search: LetterboxdSearchProvider,
        val metadata: LetterboxdMetadataProvider,
        val catalog: LetterboxdCatalogProvider,
        val crossMatch: LetterboxdCrossMatcher,
        val tracker: LetterboxdTracker,
    )

    private suspend fun components(context: Context): Components =
        componentsMutex.withLock {
            components?.let { return it }

            val readClient = LetterboxdClient.createCachedClient(context)
            val api = LetterboxdApi(readClient = readClient, writeClient = writeClient, tokens = tokens)

            val search = LetterboxdSearchProvider(api = api, providerId = id)
            val metadata = LetterboxdMetadataProvider(api = api, providerId = id)
            val idResolver = FilmIdResolver(api = api)

            Components(
                readClient = readClient,
                search = search,
                metadata = metadata,
                catalog = LetterboxdCatalogProvider(api = api, tokens = tokens, providerId = id),
                // Reuses the same search and metadata instances rather than building a second pair.
                crossMatch =
                    LetterboxdCrossMatcher(
                        searchApi = search,
                        metadataApi = metadata,
                        idResolver = idResolver,
                    ),
                tracker =
                    LetterboxdTracker(
                        api = api,
                        idResolver = idResolver,
                        tokens = tokens,
                        settings = settings,
                        providerId = id,
                    ),
            ).also { components = it }
        }

    override suspend fun getCatalogApi(context: Context): CatalogProviderApi = components(context).catalog

    override suspend fun getSearchApi(context: Context): SearchProviderApi = components(context).search

    override suspend fun getMetadataApi(context: Context): MediaMetadataProviderApi = components(context).metadata

    override suspend fun getCrossMatchApi(context: Context): CrossMatchProviderApi = components(context).crossMatch

    override suspend fun getTrackerApi(context: Context): TrackerProviderApi {
        // Refresh here so the host never receives an API that is certain to 401.
        tokens.memberBearer()
        return components(context).tracker
    }

    override suspend fun onUnload(context: Context) {
        components?.readClient?.cache?.evictAll()
        writeClient.dispatcher.executorService.shutdown()
        writeClient.connectionPool.evictAll()
        components = null
    }

    @Composable
    override fun SettingsScreen() =
        LetterboxdSettingsScreen(
            settings = settings,
            onSignIn = ::signIn,
            onSignOut = ::signOut,
        )

    /**
     * Sign-in happens entirely in the settings screen via the Password grant, so there is
     * no browser hand-off and no redirect to catch. The credentials are used for this one
     * request and never persisted.
     */
    private suspend fun signIn(
        username: String,
        password: String,
    ) {
        val token = authService.signIn(username = username, password = password)
        tokens.storeMemberToken(token)

        // The profile is what member catalogs and list URLs are keyed on.
        runCatching {
            val body = authApi.get(path = "me", requireMember = true)
            fromJson<MemberAccountDto>(body).member?.toMember()
        }.onSuccess { member ->
            if (member == null) {
                errorLog("Letterboxd: signed in but the account response carried no member.")
            } else {
                tokens.storeProfile(member)
            }
        }.onFailure {
            errorLog("Letterboxd: could not load your profile — ${it.message}")
        }
    }

    private suspend fun signOut() {
        tokens.signOut()
    }
}
