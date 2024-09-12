package com.flxProviders.sudoflix

import android.content.Context
import com.flixclusive.provider.FlixclusiveProvider
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import com.flxProviders.sudoflix.api.SudoFlixApi
import okhttp3.OkHttpClient

@FlixclusiveProvider
class SudoFlix : Provider() {
    override fun getApi(
        context: Context,
        client: OkHttpClient
    ): ProviderApi {
        return SudoFlixApi(
            client = client,
            provider = this
        )
    }
}
