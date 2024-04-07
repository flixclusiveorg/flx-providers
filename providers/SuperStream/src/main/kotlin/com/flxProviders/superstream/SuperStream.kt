package com.flxProviders.superstream

import android.content.Context
import com.flixclusive.provider.FlixclusiveProvider
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import com.flxProviders.superstream.api.SuperStreamApi
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
class SuperStream : Provider() {

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
        return SuperStreamApi(client, this)
    }
}
