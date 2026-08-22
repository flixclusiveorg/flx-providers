package com.flixclusive.provider.app.discord

import android.content.Context
import androidx.compose.runtime.Composable
import com.flixclusive.provider.FlixclusiveProvider
import com.flixclusive.provider.ProviderPlugin
import com.flixclusive.provider.app.discord.core.config.DiscordConfig
import com.flixclusive.provider.app.discord.core.network.DiscordAuthService
import com.flixclusive.provider.app.discord.core.network.DiscordExternalAssets
import com.flixclusive.provider.app.discord.core.network.DiscordTokenProvider
import com.flixclusive.provider.app.discord.core.presence.GatewayPresenceClient
import com.flixclusive.provider.app.discord.feature.settings.DiscordSettingsScreen
import com.flixclusive.provider.app.discord.feature.tracker.DiscordTracker
import com.flixclusive.provider.capability.TrackerProviderApi
import okhttp3.OkHttpClient

@FlixclusiveProvider
class DiscordPlugin : ProviderPlugin() {
    private val httpClient by lazy { OkHttpClient() }
    private val authService by lazy { DiscordAuthService(httpClient) }
    private val tokenProvider by lazy { DiscordTokenProvider(settings, authService) }
    private val externalAssets by lazy { DiscordExternalAssets(httpClient) }
    private val presence by lazy { GatewayPresenceClient(settings, tokenProvider, externalAssets) }
    private val tracker by lazy { DiscordTracker(presence, settings) }

    override suspend fun getTrackerApi(context: Context): TrackerProviderApi {
        tokenProvider.bearer()
        return tracker
    }

    override suspend fun onUnload(context: Context) {
        presence.disconnect()
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }

    @Composable
    override fun SettingsScreen() =
        DiscordSettingsScreen(
            settings = settings,
            providerId = id,
            authService = authService,
            onCodeReceived = ::exchangeCode,
        )

    private suspend fun exchangeCode(
        code: String,
        verifier: String,
    ) {
        val token =
            authService.exchangeCode(
                code = code,
                verifier = verifier,
                redirectUri = DiscordConfig.redirectUri(id),
            )

        tokenProvider.store(token)
    }
}
