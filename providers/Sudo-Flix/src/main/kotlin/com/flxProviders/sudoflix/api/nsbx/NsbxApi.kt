package com.flxProviders.sudoflix.api.nsbx

import android.content.Context
import com.flixclusive.provider.Provider
import okhttp3.OkHttpClient

internal class NsbxApi(
    client: OkHttpClient,
    context: Context,
    provider: Provider
) : AbstractNsbxApi(
    client = client,
    context = context,
    provider = provider
) {
    override val baseUrl = "https://api.nsbx.ru"
    override val name = "NSBX"
    override val streamSourceUrl = "$baseUrl/provider"
}