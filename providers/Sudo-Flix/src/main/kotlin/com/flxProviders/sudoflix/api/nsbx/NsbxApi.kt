package com.flxProviders.sudoflix.api.nsbx

import com.flixclusive.provider.Provider
import okhttp3.OkHttpClient

internal class NsbxApi(
    client: OkHttpClient,
    provider: Provider
) : AbstractNsbxApi(client, provider) {
    override val baseUrl = "https://api.nsbx.ru"
    override val name = "NSBX"
    override val streamSourceUrl = "$baseUrl/provider"
}