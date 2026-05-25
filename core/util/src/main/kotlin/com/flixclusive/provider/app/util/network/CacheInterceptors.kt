package com.flixclusive.provider.app.util.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.Interceptor

fun getOnlineInterceptor(cacheMaxAge: Int) = Interceptor { chain ->
    val response = chain.proceed(chain.request())
    response.newBuilder()
        .header("Cache-Control", "public, max-age=$cacheMaxAge")
        .removeHeader("Pragma") // strip anti-cache headers from server
        .build()
}

fun getOfflineInterceptor(
    context: Context,
    cacheMaxStale: Int
) = Interceptor { chain ->
    var request = chain.request()
    if (!context.isNetworkAvailable()) {
        request = request.newBuilder()
            .header("Cache-Control", "public, only-if-cached, max-stale=$cacheMaxStale")
            .build()
    }
    chain.proceed(request)
}

@SuppressLint("MissingPermission")
private fun Context.isNetworkAvailable(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return cm.activeNetwork?.let {
        cm.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } ?: false
}