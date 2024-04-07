package com.github.yournamehere.my_first_provider

import android.content.Context
import androidx.compose.runtime.Composable
import com.flixclusive.provider.FlixclusiveProvider
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
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
class MyFirstProvider : Provider() {

    /**
     * TIP: This is an optional method to override
     * if your provider won't require any settings screen.
     *
     *
     * Displays the settings screen for the provider.
     */
    @Composable
    override fun SettingsScreen() {
        // TODO("OPTIONAL: Not yet implemented")

        // Create a custom component for code readability
        ExampleSettingsScreen(resources = resources)
    }

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
        return MyFirstProviderApi(client, this)
    }

    /**
     * TIP: This is an optional function to override.
     *
     * Allows additional block of code to be ran first
     * before unloading this [Provider].
     *
     *
     * @param context The [Context] of the app.
     */
    override fun onUnload(context: Context?) {
        super.onUnload(context)
    }
}
