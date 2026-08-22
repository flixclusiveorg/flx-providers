package com.flixclusive.provider.app.discord.core.presence

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.util.log.debugLog
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.provider.app.discord.core.config.DiscordConfig
import com.flixclusive.provider.app.discord.core.config.PrefsKey
import com.flixclusive.provider.app.discord.core.model.DiscordActivity
import com.flixclusive.provider.app.discord.core.network.DiscordExternalAssets
import com.flixclusive.provider.app.discord.core.network.DiscordTokenProvider
import com.flixclusive.provider.extensions.getBool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

internal class GatewayPresenceClient(
    private val settings: DataStore<Preferences>,
    private val tokenProvider: DiscordTokenProvider,
    private val externalAssets: DiscordExternalAssets,
    private val rateLimiter: PresenceRateLimiter = PresenceRateLimiter(),
) : DiscordPresenceClient {
    private val client =
        OkHttpClient
            .Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(0, TimeUnit.MILLISECONDS)
            .build()

    private val scope = CoroutineScope(SupervisorJob())

    private val gateway =
        DiscordGateway(
            client = client,
            scope = scope,
            tokenProvider = tokenProvider::bearer,
            onAuthFailure = tokenProvider::forceRefresh,
            onFatal = { errorLog("Discord: $it") },
        )

    override suspend fun update(activity: DiscordActivity) {
        if (!isEnabled()) return

        rateLimiter.acquire()

        val payload = presencePayload(activity.withResolvedArtwork())
        debugLog("Discord: presence payload $payload")

        gateway.setPresence(payload)
    }

    override suspend fun clear() {
        rateLimiter.reset()
        gateway.setPresence(presencePayload(null))
    }

    override suspend fun disconnect() {
        rateLimiter.reset()

        runCatching { gateway.close() }
            .onFailure { warnLog("Discord: gateway did not close cleanly — ${it.message}") }

        scope.cancel()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    private suspend fun isEnabled() = settings.getBool(PrefsKey.ENABLED, defValue = true)

    private suspend fun DiscordActivity.withResolvedArtwork(): DiscordActivity {
        val image = largeImage ?: return this
        val bearer = tokenProvider.bearer() ?: return copy(largeImage = null)

        return copy(largeImage = externalAssets.resolve(image, bearer))
    }

    private fun presencePayload(activity: DiscordActivity?): JSONObject =
        JSONObject()
            .put("since", 0)
            .put("activities", activity?.let { JSONArray().put(it.toJson()) } ?: JSONArray())
            .put("status", "online")
            .put("afk", false)

    private fun DiscordActivity.toJson(): JSONObject {
        val json =
            JSONObject()
                .put("name", name)
                .put("type", type)
                .put("application_id", DiscordConfig.CLIENT_ID)

        details?.let { json.put("details", it) }
        state?.let { json.put("state", it) }

        if (startTimestamp != null) {
            val timestamps = JSONObject().put("start", startTimestamp)
            endTimestamp?.let { timestamps.put("end", it) }
            json.put("timestamps", timestamps)
        }

        if (largeImage != null) {
            val assets = JSONObject().put("large_image", largeImage)
            largeText?.let { assets.put("large_text", it) }
            json.put("assets", assets)
        }

        val shown = buttons.take(DiscordActivity.MAX_BUTTONS)
        if (shown.isNotEmpty()) {
            json.put("buttons", JSONArray().apply { shown.forEach { put(it.label) } })
            json.put(
                "metadata",
                JSONObject().put("button_urls", JSONArray().apply { shown.forEach { put(it.url) } }),
            )
        }

        return json
    }
}
