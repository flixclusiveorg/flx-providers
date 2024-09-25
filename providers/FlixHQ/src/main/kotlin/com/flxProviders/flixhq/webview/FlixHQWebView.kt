package com.flxProviders.flixhq.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withDefaultContext
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withMainContext
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.UserAgentManager
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.film.FilmDetails
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.provider.webview.ProviderWebView
import com.flxProviders.flixhq.api.FlixHQApi
import com.flxProviders.flixhq.api.dto.FlixHQInitialSourceData
import com.flxProviders.flixhq.extractors.rabbitstream.dto.VidCloudKey
import com.flxProviders.flixhq.webview.util.INJECTOR_SCRIPT
import com.flxProviders.flixhq.webview.util.getMediaId
import com.flxProviders.flixhq.webview.util.setup
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import java.net.URLDecoder
import kotlin.time.Duration.Companion.seconds

@SuppressLint("ViewConstructor")
@Suppress("SpellCheckingInspection")
class FlixHQWebView(
    private val mClient: OkHttpClient,
    private val api: FlixHQApi,
    context: Context,
) : ProviderWebView(context) {
    override val isHeadless = true
    override val name = "FlixHQ WebView"

    private var key: VidCloudKey? = null
    private var injectorScript = INJECTOR_SCRIPT
    private var userAgent = UserAgentManager.getRandomUserAgent()

    private val chromeClient = object: WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            return safeCall {
                val message = consoleMessage?.message()
                    ?: return@safeCall false

                if (message.contains("e4Key")) {
                    key = fromJson<VidCloudKey>(message)
                    return@safeCall true
                }

                false
            } ?: false
        }
    }

    private val client = object: WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            // To block ads
            val allowUrl = request?.url.toString().contains("flixhq")
                || request?.url.toString().contains("rabbitstream")
                || request?.url.toString().contains("javascript:")

            return if (allowUrl) {
                super.shouldOverrideUrlLoading(view, request)
            } else true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            infoLog("[FHQWebView] Injecting script on $url...")
            view?.evaluateJavascript(injectorScript, null)
        }
    }

    init {
        setup(
            client = client,
            chromeClient = chromeClient,
            userAgent = userAgent
        )
    }

    private fun setXrax(xrax: String) {
        injectorScript = INJECTOR_SCRIPT
            .replace("__rabbit_id__", xrax)
            .replace("__user_agent__", userAgent)
    }

    override suspend fun getLinks(
        watchId: String,
        film: FilmDetails,
        episode: Episode?,
        onLinkFound: (MediaLink) -> Unit
    ) {
        infoLog("[FHQWebView] Getting links for ${film.title}")
        val watchIdToUse = withIOContext {
            api.getMediaId(film = film)
        } ?: throw Exception("Can't find watch id!")

        val servers = withIOContext {
            api.getEpisodeIdAndServers(
                watchId = watchIdToUse,
                episode = episode?.number,
                season = episode?.season
            )
        }

        val validServers = servers.filter {
            it.first.equals("vidcloud", true)
                || it.first.equals("upcloud", true)
        }

        if (validServers.isEmpty())
            throw Exception("No valid servers found!")

        validServers.forEach { (serverName, serverEmbedUrl) ->
            val initialSourceData = withIOContext {
                mClient.request(
                    url = "${api.baseUrl}/ajax/episode/sources/${serverEmbedUrl.split('.').last()}",
                    userAgent = userAgent
                ).execute().body?.string()
            } ?: throw Exception("Can't find decryption key!")

            val serverUrl = withDefaultContext {
                URLDecoder.decode(
                    fromJson<FlixHQInitialSourceData>(initialSourceData).link,
                    "UTF-8"
                )
            }

            val xrax = serverUrl
                .split('/').last()
                .split('?').first()
            val extractor = api.extractors[serverName.lowercase()]
                ?: throw Exception("Extractor not found!")

            infoLog("[FHQWebView] XRAX: $xrax")
            setXrax(xrax)
            withMainContext {
                loadUrl(extractor.baseUrl)
            }

            withTimeout(60.seconds) {
                waitForKeyToBeAttached()
            }

            requireNotNull(key) {
                "Can't find decryption key!"
            }

            infoLog("[FHQWebView] Extracting links...")
            extractor.extract(
                url = serverUrl,
                key = key!!,
                userAgent = userAgent,
                onLinkFound = onLinkFound
            )
        }
    }

    private suspend fun waitForKeyToBeAttached() {
        infoLog("[FHQWebView] Waiting for key to be attached...")
        key = null
        while(key == null)
            delay(500)
    }
}