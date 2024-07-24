package com.flxProviders.sudoflix.api.nsbx

import okhttp3.OkHttpClient

internal class NsbxApi(
    client: OkHttpClient
) : AbstractNsbxApi(client) {
    override val baseUrl = "https://api.nsbx.ru"
    override val name = "NSBX"
    override val streamSourceUrl = "$baseUrl/provider"
}