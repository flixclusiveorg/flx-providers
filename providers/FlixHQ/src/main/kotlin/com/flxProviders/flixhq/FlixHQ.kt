package com.flxProviders.flixhq

import android.app.Activity
import android.content.Context
import com.flixclusive.provider.FlixclusiveProvider
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import com.flxProviders.flixhq.api.FlixHQApi
import com.flxProviders.flixhq.extractors.vidcloud.dto.VidCloudKey
import okhttp3.OkHttpClient

@FlixclusiveProvider
class FlixHQ : Provider() {

    override fun getApi(
        context: Context?,
        client: OkHttpClient
    ) = FlixHQApi(client = client)
}
