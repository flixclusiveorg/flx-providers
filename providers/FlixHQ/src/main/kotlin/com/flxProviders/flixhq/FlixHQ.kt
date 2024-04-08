package com.flxProviders.flixhq

import android.content.Context
import com.flixclusive.provider.FlixclusiveProvider
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import com.flxProviders.flixhq.extractors.vidcloud.dto.VidCloudKey
import com.flxProviders.flixhq.api.FlixHQApi
import okhttp3.OkHttpClient

/**
 * ## The main class for a Flixclusive provider.
 *
 * A [Provider] acts as a middleman between your [Provider]
 * and the application. It facilitates the lifecycle of your [Provider],
 * ensuring seamless integration with the application.
 *
 * #### WARN: Every provider must be annotated with [FlixclusiveProvider].
 *
 * To create a provider, extend this class and override the necessary methods.
 *
 * @see [TODO: ADD DOCS LINK FOR PLUGIN CREATION !!!!!]
 */
@FlixclusiveProvider
class FlixHQ : Provider() {
    /**
     * Return the [ProviderApi] in here to let
     * the app handle the provider management.
     *
     *
     * @param context The [Context] of the app.
     * @param client A _dirty_ instance of [OkHttpClient] of the app.
     */
    override fun getApi(
        context: Context?,
        client: OkHttpClient
    ): ProviderApi {
        val key = settings.getObject("key", VidCloudKey())

        return FlixHQApi(
            client = client,
            key = key
        )
    }
}
