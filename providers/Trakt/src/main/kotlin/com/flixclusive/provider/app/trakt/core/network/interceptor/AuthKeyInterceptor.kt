package com.flixclusive.provider.app.trakt.core.network.interceptor

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.provider.app.trakt.core.config.PrefsKey
import com.flixclusive.provider.app.trakt.core.model.AuthToken
import com.flixclusive.provider.extensions.getString
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor

internal class AuthKeyInterceptor(
    private val prefs: DataStore<Preferences>
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val key = runBlocking {
            prefs.getString(PrefsKey.PREFS_AUTH, null)
                ?.let { safeCall { fromJson<AuthToken?>(it) } }
        }

        val request = chain.request()
        val newRequest = request.newBuilder()
            .addHeader("Authorization", "Bearer ${key?.accessToken}")
            .build()
        
        return chain.proceed(newRequest)
    }
}