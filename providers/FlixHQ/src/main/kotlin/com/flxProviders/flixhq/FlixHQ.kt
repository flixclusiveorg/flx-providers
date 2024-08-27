package com.flxProviders.flixhq

import android.content.Context
import com.flixclusive.provider.FlixclusiveProvider
import com.flixclusive.provider.Provider
import com.flxProviders.flixhq.api.FlixHQApi
import okhttp3.OkHttpClient

@FlixclusiveProvider
class FlixHQ : Provider() {

    override fun getApi(
        context: Context,
        client: OkHttpClient
    ) = FlixHQApi(
        client = client,
        context = context,
        provider = this
    )
}
