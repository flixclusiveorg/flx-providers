package com.flixclusive.provider.app.trakt.core.network.util

import com.flixclusive.core.util.coroutines.FlxDispatchers
import okhttp3.OkHttpClient
import okhttp3.Request

internal object TypesenseKeyFetcher {
    suspend fun OkHttpClient.fetchTypesenseSearchKey(): String? {
        return FlxDispatchers.withIOContext {
            val request = Request.Builder()
                .url("https://app.trakt.tv/search")
                .build()

            val html = newCall(request).execute().use { response ->
                response.body.string()
            }

            Regex("""typesense:\{keys:\{media:\{default:"(.*)",""")
                .find(html)
                ?.groupValues
                ?.get(1)
        }
    }
}