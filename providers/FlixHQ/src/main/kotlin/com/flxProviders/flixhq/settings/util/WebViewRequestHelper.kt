package com.flxProviders.flixhq.settings.util

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.flxProviders.flixhq.settings.SHAWSHANK_REDEMPTION_WATCH_LINK
import java.net.URL
import javax.net.ssl.HttpsURLConnection

fun interceptE4Request(
    request: WebResourceRequest
): WebResourceResponse {
    val requestUrl = URL(request.url.toString())
    val conn = requestUrl.openConnection() as HttpsURLConnection
    for ((key, value) in request.requestHeaders) {
        conn.addRequestProperty(key, value)
    }

    conn.setRequestProperty("Referer", SHAWSHANK_REDEMPTION_WATCH_LINK)

    return WebResourceResponse(
        conn.contentType.substringBefore(";"),
        conn.contentType.substringAfter("charset=", "UTF-8"),
        conn.inputStream
    )
}