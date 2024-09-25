package com.flxProviders.superstream.api.util

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.widget.Toast
import com.flixclusive.core.util.android.showToast
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.runOnDefault
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.runOnMain
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.network.okhttp.UserAgentManager
import com.flixclusive.core.util.network.okhttp.WebViewInterceptor
import com.flxProviders.superstream.api.util.CookieHelper.getValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.seconds

/**
 * If a CloudFlare security verification redirection is detected, execute a
 * webView and retrieve the necessary headers.
 *
 * Reference code from [NovelDokusha](https://github.com/nanihadesuka/NovelDokusha/blob/ecca3fb4a62479d5187bbedee9c0efae81050f2c/networking/src/main/java/my/noveldokusha/network/interceptors/CloudfareVerificationInterceptor.kt#L33)
 */
@SuppressLint("SetJavaScriptEnabled")
@Suppress("unused")
internal class CloudflareWebViewInterceptor(
    context: Context
) : WebViewInterceptor(context) {
    private val lock = ReentrantLock()

    private val errorCodes = listOf(
        HttpsURLConnection.HTTP_FORBIDDEN /*403*/,
        HttpsURLConnection.HTTP_UNAVAILABLE /*503*/
    )
    private val serverList = listOf("cloudflare-nginx", "cloudflare")
    private val cfCookieKey = "cf_clearance"

    override val isHeadless = false
    override val name = "Cloudflare Resolver"

    init {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)
    }


    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!response.isCloudFlare()) {
            return response
        }

        return lock.withLock {
            try {
                response.close()
                runOnMain {
                    settings.userAgentString = request.header("user-agent")
                        ?: UserAgentManager.getRandomUserAgent()
                }

                val cloudflareUrl = request.url.toString()
                val oldClearance = cookieManager.getValue(
                    key = cfCookieKey,
                    url = cloudflareUrl
                )

                if (oldClearance != null) {
                    infoLog("[CF] Trying old cf_clearance...")
                    safeCall("[CF] Old cf_clearance might be outdated!") {
                        return@withLock chain.bypassChallenge(cookieManager)
                    }

                    runOnMain {
                        cookieManager.removeAllCookies(null)
                        cookieManager.flush()
                    }
                }

                infoLog("[CF] Resolving with WebView...")
                try {
                    runOnDefault {
                        withTimeout(60.seconds) {
                            resolveWithWebView(request, cookieManager)
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    runOnMain {
                        context.showToast(
                            message = "Cloudflare resolver timed out!",
                            duration = Toast.LENGTH_LONG
                        )
                    }

                    throw Exception("[CF] WebView timed out after 60 seconds")
                }


                infoLog("[CF] Using new cf_clearance cookie...")
                chain.bypassChallenge(cookieManager)
            } catch (e: CancellationException) {
                errorLog(e)
                throw e
            } catch (e: IOException) {
                errorLog(e)
                throw e
            } catch (e: Exception) {
                errorLog(e)
                throw IOException(e.message, e.cause)
            }
        }
    }

    private fun Response.isCloudFlare(): Boolean {
        return code in errorCodes
            && header("server") in serverList
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveWithWebView(
        request: Request,
        cookieManager: CookieManager
    ) {
        val cloudflareUrl = request.url.toString()
        val headers = request
            .headers
            .toMultimap()
            .mapValues { it.value.firstOrNull() ?: "" }

        loadUrl(cloudflareUrl, headers)

        while (true) {
            val clearance = cookieManager.getValue(
                key = cfCookieKey,
                url = cloudflareUrl
            )

            if (clearance != null) {
                return
            }
        }
    }

    private fun Interceptor.Chain.bypassChallenge(cookieManager: CookieManager): Response {
        val request = request()
        val url = request.url.toString()
        val response = proceed(
            request.newBuilder()
                .addHeader("Cookie", cookieManager.getCookie(url))
                .build()
        )

        if (response.isCloudFlare()) {
            response.close()
            throw Exception("[CF] Could not bypass verification")
        }

        return response
    }
}