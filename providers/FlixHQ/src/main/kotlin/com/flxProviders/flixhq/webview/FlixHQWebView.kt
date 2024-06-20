package com.flxProviders.flixhq.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.flixclusive.core.util.R
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.debugLog
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.USER_AGENT
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceDataState
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.provider.util.FlixclusiveWebView
import com.flixclusive.provider.util.WebViewCallback
import com.flxProviders.flixhq.api.FlixHQApi
import com.flxProviders.flixhq.api.dto.FlixHQInitialSourceData
import com.flxProviders.flixhq.extractors.rabbitstream.dto.VidCloudKey
import com.flxProviders.flixhq.webview.util.getMediaId
import com.flxProviders.flixhq.webview.util.setup
import com.flxProviders.flixhq.webview.util.toReferer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.net.URLDecoder

@Suppress("SpellCheckingInspection")
internal const val INJECTOR_SCRIPT = "javascript:(function() {  function shift(y) {      return [          (4278190080 & y) >> 24,          (16711680 & y) >> 16,          (65280 & y) >> 8,          255 & y      ];  }  function shiftArray(toShift, shiftNums) {      try {          for (let i = 0; i < toShift.length; i++) {              toShift[i] = toShift[i] ^ shiftNums[i % shiftNums.length];          }      } catch (err) {          return null;      }  }  function checkClipboard() {    try {      var iframeWindow = window;      if (iframeWindow.clipboard) {        clearInterval(pollingInterval);        const browserVersion = iframeWindow.browser_version;        const kId = iframeWindow.localStorage.getItem('kid');        const kVersion = iframeWindow.localStorage.getItem('kversion');        const arrayKeys = new Uint8Array(iframeWindow.clipboard());        shiftArray(arrayKeys, shift(parseInt(kVersion)));        const finalKeys = btoa(String.fromCharCode.apply(null, new Uint8Array(arrayKeys)));        var body = document.body;        body.innerHTML = `<div style='text-align: center;'><h1 style='font-size: 60px; color: white'>Your E4 keys are:</h1><p style='font-weight: bold; font-size: 30px; color: white'>`+finalKeys+`</p><h1 style='font-size: 60px; color: white'>Other details are:</h1><p style='font-weight: bold; font-size: 30px; color: white'>BrowserVersion = `+browserVersion+`</p><br/><p style='font-weight: bold; font-size: 30px; color: white'>ID = `+kId+`</p><br/><p style='font-weight: bold; font-size: 30px; color: white'>Version = `+kVersion+`</p></div>`;                console.log(`{'e4Key': '`+finalKeys+`', 'browserVersion': '`+browserVersion+`', 'kVersion': '`+kVersion+`', 'kId': '`+kId+`'}`);      } else {        setTimeout(checkClipboard, 200);      }    } catch (error) {      setTimeout(checkClipboard, 500);    }  }  var pollingInterval = setInterval(checkClipboard, 500);})();"

@SuppressLint("ViewConstructor")
@Suppress("SpellCheckingInspection")
class FlixHQWebView(
    private val mClient: OkHttpClient,
    private val api: FlixHQApi,

    private val filmToScrape: FilmDetails,
    private val episodeData: Episode?,
    private val callback: WebViewCallback,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    context: Context,
) : FlixclusiveWebView(
    filmToScrape = filmToScrape,
    context = context,
    callback = callback,
    episodeData = episodeData
) {
    private var key: VidCloudKey? = null

    private val chromeClient = object: WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            return safeCall {
                val message = consoleMessage?.message()
                    ?: return@safeCall false

                if (!message.contains("e4Key"))
                    return@safeCall false

                key = fromJson<VidCloudKey>(message)
                return true
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
    }

    init {
        setup(
            client = client,
            chromeClient = chromeClient
        )
    }

    override suspend fun startScraping() {
        try {
            val watchId = withContext(ioDispatcher) {
                api.getMediaId(film = filmToScrape)
            } ?: return callback.updateDialogState(SourceDataState.Unavailable())

            val servers = withContext(ioDispatcher) {
                api.getEpisodeIdAndServers(
                    watchId = watchId,
                    episode = episodeData?.number,
                    season = episodeData?.season
                )
            }

            val validServers = servers.filter {
                it.first.equals("vidcloud", true)
                || it.first.equals("upcloud", true)
            }

            if (validServers.isEmpty())
                throw Exception("No valid servers found!")

            debugLog("Valid servers: $validServers")
            validServers.mapAsync { (serverName, serverEmbedUrl) ->
                val initialSourceData = withContext(ioDispatcher) {
                    mClient.request(url = "${api.baseUrl}/ajax/episode/sources/${serverEmbedUrl.split('.').last()}").execute().body?.string()
                } ?: throw Exception("Can't find decryption key!")

                callback.updateDialogState(SourceDataState.Extracting(UiText.StringResource(R.string.extracting_from_provider_format, "FlixHQ")))

                val serverUrl = withContext(ioDispatcher) {
                    URLDecoder.decode(
                        fromJson<FlixHQInitialSourceData>(initialSourceData).link,
                        "UTF-8"
                    )
                }

                waitForKeyToBeAttached(
                    serverUrl = serverUrl,
                    filmId = watchId
                )

                val extractor = api.extractors[serverName.lowercase()]
                    ?: throw Exception("Extractor not found!")

                withContext(ioDispatcher) {
                    extractor.key = key!!
                    extractor.extract(
                        url = serverUrl,
                        onLinkLoaded = callback::onLinkLoaded,
                        onSubtitleLoaded = callback::onSubtitleLoaded
                    )
                    key = null
                }
            }

            return callback.onSuccess(episodeData)
        } catch (e: Exception) {
            errorLog(e.stackTraceToString())
            return callback.updateDialogState(
                SourceDataState.Error(
                    UiText.StringValue("[${api.name}]> ${e.localizedMessage}")
                )
            )
        }
    }

    private suspend fun waitForKeyToBeAttached(
        serverUrl: String,
        filmId: String
    ) {
        val headers = mapOf(
            "Referer" to api.baseUrl + filmId.toReferer(),
            "User-Agent" to USER_AGENT,
        )
        loadUrl(serverUrl, headers)
        delay(1000)
        loadUrl(INJECTOR_SCRIPT)

        var retries = 0
        val maxRetries = 30 // seconds
        while(key == null && retries < maxRetries) {
            delay(1000)
            retries++
        }

        if (key == null)
            throw Exception("Can't find decryption key!")
    }
}