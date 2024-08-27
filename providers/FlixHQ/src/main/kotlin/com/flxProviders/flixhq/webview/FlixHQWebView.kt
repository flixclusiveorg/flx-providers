package com.flxProviders.flixhq.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.flixclusive.core.util.common.dispatcher.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.common.dispatcher.AppDispatchers.Companion.withMainContext
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.network.USER_AGENT
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.MediaLink
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.provider.webview.ProviderWebView
import com.flxProviders.flixhq.api.FlixHQApi
import com.flxProviders.flixhq.api.dto.FlixHQInitialSourceData
import com.flxProviders.flixhq.extractors.rabbitstream.dto.VidCloudKey
import com.flxProviders.flixhq.webview.util.INJECTOR_SCRIPT
import com.flxProviders.flixhq.webview.util.getMediaId
import com.flxProviders.flixhq.webview.util.setup
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.net.URLDecoder

@SuppressLint("ViewConstructor")
@Suppress("SpellCheckingInspection")
class FlixHQWebView(
    private val mClient: OkHttpClient,
    private val api: FlixHQApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    context: Context,
) : ProviderWebView(context) {
    override val isHeadless = true
    override val name = "FlixHQ WebView"

    private var key: VidCloudKey? = null
    private var injectorScript = INJECTOR_SCRIPT

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
            chromeClient = chromeClient
        )
    }

    private fun setXrax(xrax: String) {
        injectorScript = INJECTOR_SCRIPT
            .replace("__rabbit_id__", xrax)
            .replace("__user_agent__", USER_AGENT)
    }

    override suspend fun getLinks(
        watchId: String,
        film: FilmDetails,
        episode: Episode?,
        onLinkFound: (MediaLink) -> Unit
    ) {
        infoLog("[FHQWebView] Getting links for ${film.title}")
        val watchIdToUse = withContext(ioDispatcher) {
            api.getMediaId(film = film)
        } ?: throw Exception("Can't find watch id!")

        val servers = withContext(ioDispatcher) {
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
                mClient.request(url = "${api.baseUrl}/ajax/episode/sources/${serverEmbedUrl.split('.').last()}").execute().body?.string()
            } ?: throw Exception("Can't find decryption key!")

            val serverUrl = withIOContext {
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

            waitForKeyToBeAttached()

            infoLog("[FHQWebView] Extracting links...")
            withIOContext {
                extractor.extract(
                    url = serverUrl,
                    key = key!!,
                    onLinkFound = onLinkFound
                )
            }
        }
    }

    private suspend fun waitForKeyToBeAttached() {
        infoLog("[FHQWebView] Waiting for key to be attached...")
        key = null
        var retries = 0
        val maxRetries = 40 // seconds
        while(key == null && retries < maxRetries) {
            delay(1000)
            retries++
        }

        if (key == null)
            throw Exception("Can't find decryption key!")
    }
}