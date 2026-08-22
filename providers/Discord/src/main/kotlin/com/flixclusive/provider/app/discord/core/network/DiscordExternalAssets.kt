package com.flixclusive.provider.app.discord.core.network

import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.core.util.log.debugLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.core.util.network.okhttp.HttpMethod
import com.flixclusive.core.util.network.okhttp.genericBodyRequest
import com.flixclusive.provider.app.discord.core.config.DiscordConfig
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject

private const val CACHE_MAX_ENTRIES = 128
private const val ASSET_PREFIX = "mp:"

internal class DiscordExternalAssets(
    private val client: OkHttpClient,
) {
    private val cache =
        object : LinkedHashMap<String, String>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > CACHE_MAX_ENTRIES
        }

    suspend fun resolve(
        imageUrl: String,
        bearer: String,
    ): String? {
        if (imageUrl.isBlank()) return null
        if (imageUrl.startsWith(ASSET_PREFIX)) return imageUrl

        synchronized(cache) { cache[imageUrl] }?.let { return it }

        return FlxDispatchers.withIOContext {
            runCatching { request(imageUrl, bearer) }
                .onFailure { warnLog("Discord: external asset request failed — ${it.message}") }
                .getOrNull()
                ?.also { resolved -> synchronized(cache) { cache[imageUrl] = resolved } }
        }
    }

    private fun request(
        imageUrl: String,
        bearer: String,
    ): String? {
        val body = JSONObject().put("urls", JSONArray().put(imageUrl)).toString()

        return client
            .genericBodyRequest(
                url = DiscordConfig.externalAssetsUrl(),
                method = HttpMethod.POST,
                body = body,
                mediaType = "application/json".toMediaType(),
                headers = Headers.headersOf("Authorization", bearer),
            ).execute()
            .use { response ->
                val payload = response.body.string()

                if (!response.isSuccessful) {
                    warnLog("Discord: external-assets returned HTTP ${response.code}; presence stays text-only.")
                    return null
                }

                val path =
                    JSONArray(payload)
                        .optJSONObject(0)
                        ?.optString("external_asset_path")
                        ?.takeIf(String::isNotBlank)

                if (path == null) {
                    warnLog("Discord: external-assets returned no asset path.")
                    return null
                }

                debugLog("Discord: resolved artwork to $ASSET_PREFIX$path")
                "$ASSET_PREFIX$path"
            }
    }
}
