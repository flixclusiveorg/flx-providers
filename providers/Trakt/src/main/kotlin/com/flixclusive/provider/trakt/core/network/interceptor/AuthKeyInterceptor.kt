package com.flixclusive.provider.trakt.core.network.interceptor

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.provider.settings.getObject
import com.flixclusive.provider.trakt.core.config.PrefsKey
import com.flixclusive.provider.trakt.core.model.AuthToken
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor

internal class AuthKeyInterceptor(
    private val prefs: DataStore<Preferences>
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val key = runBlocking {
            prefs.getObject<AuthToken>(PrefsKey.PREFS_AUTH, null)
                ?: throw IllegalStateException("Auth key is not set in preferences")
        }

        val request = chain.request()
        val newRequest = request.newBuilder()
            .addHeader("Authorization", "Bearer ${key.accessToken}")
            .build()
        
        return chain.proceed(newRequest)
    }
}