package com.flixclusive.provider.app.trakt

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.provider.FlixclusiveProvider
import com.flixclusive.provider.ProviderPlugin
import com.flixclusive.provider.app.trakt.core.config.PrefsKey
import com.flixclusive.provider.app.trakt.core.config.TraktApiConfig
import com.flixclusive.provider.app.trakt.core.config.TypeSenseKeyProvider
import com.flixclusive.provider.app.trakt.core.datastore.observeAuthToken
import com.flixclusive.provider.app.trakt.core.datastore.observeSettingToggles
import com.flixclusive.provider.app.trakt.core.model.AuthToken
import com.flixclusive.provider.app.trakt.core.network.TraktApiService
import com.flixclusive.provider.app.trakt.core.network.util.OkHttpClientUtil
import com.flixclusive.provider.app.trakt.core.network.util.TypesenseKeyFetcher.fetchTypesenseSearchKey
import com.flixclusive.provider.app.trakt.core.theme.TraktTheme
import com.flixclusive.provider.app.trakt.feature.auth.AuthState
import com.flixclusive.provider.app.trakt.feature.auth.ui.AuthGuardScreen
import com.flixclusive.provider.app.trakt.feature.auth.user.UserScreen
import com.flixclusive.provider.app.trakt.feature.auth.user.UserState
import com.flixclusive.provider.app.trakt.feature.auth.util.ObserveOauthDeepLinkUri
import com.flixclusive.provider.app.trakt.feature.catalog.TraktCatalog
import com.flixclusive.provider.app.trakt.feature.crossmatch.TraktCrossMatcher
import com.flixclusive.provider.app.trakt.feature.metadata.TraktMetadata
import com.flixclusive.provider.app.trakt.feature.search.TraktSearch
import com.flixclusive.provider.app.trakt.feature.tracker.TraktTracker
import com.flixclusive.provider.capability.CatalogProviderApi
import com.flixclusive.provider.capability.CrossMatchProviderApi
import com.flixclusive.provider.capability.MediaLinkProviderApi
import com.flixclusive.provider.capability.MediaMetadataProviderApi
import com.flixclusive.provider.capability.SearchProviderApi
import com.flixclusive.provider.capability.TrackerProviderApi
import com.flixclusive.provider.settings.getBool
import com.flixclusive.provider.settings.getObject
import com.flixclusive.provider.settings.setBool
import com.flixclusive.provider.settings.setObject
import com.flixclusive.provider.settings.setString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

@FlixclusiveProvider
class TraktPlugin : ProviderPlugin(), TypeSenseKeyProvider {
    private val authError = MutableSharedFlow<String>()
    private val userFlow = MutableStateFlow<UserState>(UserState.LoggingOut)

    private var exchangeTokenJob: Job? = null
    private var fetchUserJob: Job? = null

    override var typeSenseKey: String? = null

    override suspend fun onUnload(context: Context) {
        OkHttpClientUtil.deleteCache()

        settings.setObject<AuthToken?>(PrefsKey.PREFS_AUTH, null)
        settings.setString(PrefsKey.PREFS_AUTH_USER_ID, null)
        userFlow.value = UserState.LoggingOut
    }

    override suspend fun reloadTypeSenseKey() {
        val client = OkHttpClientUtil.createVanillaClient()
        typeSenseKey = client.fetchTypesenseSearchKey()
        if (typeSenseKey == null) {
            throw RuntimeException("Failed to fetch TypeSense search key for Trakt cross-matching.")
        }
    }

    override suspend fun getTrackerApi(context: Context): TrackerProviderApi {
        return TraktTracker(
            plugin = this,
            context = context,
            settings = settings,
        )
    }

    override suspend fun getMediaLinkApi(context: Context): MediaLinkProviderApi {
        TODO("Uncomment when `watchnow` endpoint is available. It currently returns 401 (unauthorized)")
//        return TraktWatchNowProvider(
//            context = context,
//            plugin = this,
//        )
    }

    override suspend fun getSearchApi(context: Context): SearchProviderApi {
        return TraktSearch(
            context = context,
            plugin = this,
            typeSenseKeyProvider = this
        )
    }

    override suspend fun getCrossMatchApi(context: Context): CrossMatchProviderApi {
        return TraktCrossMatcher(
            context = context,
            plugin = this,
            typeSenseKeyProvider = this
        )
    }

    override suspend fun getMetadataApi(context: Context): MediaMetadataProviderApi {
        return TraktMetadata(
            context = context,
            plugin = this,
        )
    }

    override suspend fun getCatalogApi(context: Context): CatalogProviderApi? {
        val isCatalogsAllowed = settings.getBool(PrefsKey.PREFS_CATALOGS, true)
        if (!isCatalogsAllowed) {
            infoLog("Catalog API is disabled in settings, returning null")
            return null
        }

        val authToken = settings.getObject<AuthToken>(PrefsKey.PREFS_AUTH)
        if (authToken == null || authToken.isExpired) {
            infoLog("No valid auth token found, Catalog API will not be available")
            return null
        }

        return TraktCatalog(
            context = context,
            plugin = this,
            authToken = authToken
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Composable
    override fun SettingsScreen() {
        val authState by settings.observeAuthToken().collectAsStateWithLifecycle(AuthState.Loading)
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(authState) {
            if (authState is AuthState.Expired) {
                val refreshCode = (authState as AuthState.Expired).data.refreshToken
                exchangeCodeForToken(refreshCode, refresh = true)
            } else if (authState is AuthState.Authenticated) {
                saveAndLoadUser()
            }
        }

        LaunchedEffect(true) {
            authError.collect {
                snackbarHostState.showSnackbar(it)
            }
        }

        ObserveOauthDeepLinkUri {
            infoLog("Received OAuth deep link: $it")

            val code = it.getQueryParameter("code") ?: return@ObserveOauthDeepLinkUri
            exchangeCodeForToken(code = code)
        }

        TraktTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    AuthGuardScreen(
                        state = authState,
                        modifier = Modifier.padding(it)
                    ) {
                        val userState by userFlow.collectAsStateWithLifecycle()
                        val toggles by settings.observeSettingToggles()
                            .collectAsStateWithLifecycle(emptyList())

                        UserScreen(
                            modifier = Modifier.fillMaxSize(),
                            userState = userState,
                            toggles = { toggles },
                            onRetry = ::saveAndLoadUser,
                            onToggle = { key, value ->
                                FlxDispatchers.launchOnIO { settings.setBool(key, value) }
                            },
                            onLogout = {
                                FlxDispatchers.launchOnIO {
                                    userFlow.value = UserState.LoggingOut
                                    settings.setObject<AuthToken?>(PrefsKey.PREFS_AUTH, null)
                                    settings.setString(PrefsKey.PREFS_AUTH_USER_ID, null)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun exchangeCodeForToken(
        code: String,
        refresh: Boolean = false
    ) {
        if (exchangeTokenJob?.isActive == true) {
            warnLog("Token exchange already in progress, ignoring new request...")
            return
        }

        val request = TraktApiConfig.getExchangeTokenRequest(
            providerId = id,
            code = code,
            isRefreshing = refresh
        )
        exchangeTokenJob = FlxDispatchers.launchOnIO {
            val client = OkHttpClientUtil.createVanillaClient()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    errorLog("Failed to exchange code for token: ${response.code} - ${response.message}")
                    authError.emit("Failed to exchange code for token: ${response.code}")
                    return@use
                }

                val responseBody = response.body.string()
                val authToken = runCatching {
                    fromJson<AuthToken>(responseBody)
                }.onFailure {
                    it.printStackTrace()
                    errorLog("Failed to parse token exchange response: ${it.message}")
                    authError.emit("Failed to parse token exchange response")
                    return@use
                }.getOrNull()

                if (authToken == null) {
                    errorLog("Received null auth token after exchange")
                    authError.emit("Failed to exchange code for token: received null token")
                    return@use
                }

                settings.setObject(PrefsKey.PREFS_AUTH, authToken)
            }
        }
    }

    private fun saveAndLoadUser() {
        if (fetchUserJob?.isActive == true) {
            warnLog("User fetch already in progress, ignoring new request...")
            return
        }

        fetchUserJob = FlxDispatchers.launchOnIO {
            userFlow.value = UserState.Loading

            val apiService = TraktApiService.create(
                OkHttpClientUtil.createNonCachedClient(settings)
            )
            val userResult = runCatching { apiService.getUser() }
            userResult.onFailure {
                it.printStackTrace()
                errorLog("Failed to load user after authentication: ${it.message}")
                authError.emit("Failed to load user after authentication")
                userFlow.value = UserState.Error(it.message ?: "Unknown error")
                return@launchOnIO
            }

            val response = userResult.getOrNull() ?: return@launchOnIO
            infoLog("Successfully loaded user after authentication: ${response.user.name}")

            settings.setString(PrefsKey.PREFS_AUTH_USER_ID, response.traktSlug)
            userFlow.value = UserState.Success(response.user)
        }
    }
}
