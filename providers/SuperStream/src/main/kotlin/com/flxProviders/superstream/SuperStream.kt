package com.flxProviders.superstream

import android.content.Context
import com.flixclusive.provider.FlixclusiveProvider
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import com.flxProviders.superstream.api.SuperStreamApi
import okhttp3.OkHttpClient

@FlixclusiveProvider
class SuperStream : Provider() {

    override fun getApi(
        context: Context?,
        client: OkHttpClient
    ): ProviderApi {
        return SuperStreamApi(client)
    }
}
