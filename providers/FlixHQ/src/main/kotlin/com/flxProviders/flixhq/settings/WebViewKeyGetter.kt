package com.flxProviders.flixhq.settings

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.debugLog
import com.flixclusive.core.util.network.USER_AGENT
import com.flixclusive.core.util.network.fromJson
import com.flxProviders.flixhq.extractors.vidcloud.dto.VidCloudKey
import com.flxProviders.flixhq.settings.util.getString
import com.flxProviders.flixhq.settings.util.interceptE4Request
import kotlinx.coroutines.delay
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.math.roundToInt


internal const val SHAWSHANK_REDEMPTION_WATCH_LINK = "https://flixhq.to/watch-movie/watch-the-shawshank-redemption-19679"
internal const val SHAWSHANK_REDEMPTION_WATCH_ID = "watch-movie/watch-the-shawshank-redemption-19679"
internal const val INJECTOR_SCRIPT = "javascript:(function() {  function shift(y) {      return [          (4278190080 & y) >> 24,          (16711680 & y) >> 16,          (65280 & y) >> 8,          255 & y      ];  }  function shiftArray(toShift, shiftNums) {      try {          for (let i = 0; i < toShift.length; i++) {              toShift[i] = toShift[i] ^ shiftNums[i % shiftNums.length];          }      } catch (err) {          return null;      }  }  function checkClipboard() {    try {      var iframeWindow = window;      if (iframeWindow.clipboard) {        clearInterval(pollingInterval);        const browserVersion = iframeWindow.browser_version;        const kId = iframeWindow.localStorage.getItem('kid');        const kVersion = iframeWindow.localStorage.getItem('kversion');        const arrayKeys = new Uint8Array(iframeWindow.clipboard());        shiftArray(arrayKeys, shift(parseInt(kVersion)));        const finalKeys = btoa(String.fromCharCode.apply(null, new Uint8Array(arrayKeys)));        var body = document.body;        body.innerHTML = `<div style='text-align: center;'><h1 style='font-size: 60px; color: white'>Your E4 keys are:</h1><p style='font-weight: bold; font-size: 30px; color: white'>`+finalKeys+`</p><h1 style='font-size: 60px; color: white'>Other details are:</h1><p style='font-weight: bold; font-size: 30px; color: white'>BrowserVersion = `+browserVersion+`</p><br/><p style='font-weight: bold; font-size: 30px; color: white'>ID = `+kId+`</p><br/><p style='font-weight: bold; font-size: 30px; color: white'>Version = `+kVersion+`</p></div>`;                console.log(`{'e4Key': '`+finalKeys+`', 'browserVersion': '`+browserVersion+`', 'kVersion': '`+kVersion+`', 'kId': '`+kId+`'}`);      } else {        setTimeout(checkClipboard, 200);      }    } catch (error) {      setTimeout(checkClipboard, 500);    }  }  var pollingInterval = setInterval(checkClipboard, 500);})();"

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
internal fun WebViewKeyGetter(
    resources: Resources,
    setKey: (key: VidCloudKey) -> Unit,
    onDismiss: () -> Unit
) {
    val localDensity = LocalDensity.current

    var e4Url: String? by remember { mutableStateOf(null) }

    var webView: WebView? by remember { mutableStateOf(null) }
    var key by remember { mutableStateOf(VidCloudKey()) }
    var isKeySet by remember { mutableStateOf(false) }
    var isPageLoadingFinished by remember { mutableStateOf(false) }
    val dismissProperly = {
        webView?.destroy()
        onDismiss()
    }

    val chromeClient = remember {
        object: WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                isKeySet = safeCall {
                    val message = consoleMessage?.message() ?: return@safeCall false

                    if (!message.contains("e4Key"))
                        return false

                    debugLog(message)
                    key = fromJson<VidCloudKey>(message)
                    true
                } ?: false

                return super.onConsoleMessage(consoleMessage)
            }
        }
    }
    val client = remember {
        object: WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                isPageLoadingFinished = false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                isPageLoadingFinished = true
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val matcher = """https://rabbitstream\.net/v2/embed-4/[a-zA-Z0-9]+(\?[a-zA-Z0-9=&-]*)?""".toRegex()

                if (matcher.matches(request?.url.toString())) {
                    if (e4Url == request?.url.toString()) {
                        return interceptE4Request(request!!)
                    }

                    e4Url = request?.url.toString()
                }

                return super.shouldInterceptRequest(view, request)
            }

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
    }

    LaunchedEffect(e4Url) {
        if (e4Url.isNullOrEmpty().not()) {
            webView?.loadUrl(e4Url!!)
            delay(1500)
            webView?.loadUrl(INJECTOR_SCRIPT)
        }
    }

    LaunchedEffect(key) {
        if (key.e4Key.isEmpty().not()) {
            setKey(key)
        }
    }

    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = dismissProperly
    ) {
        Surface(
            modifier = Modifier
                .padding(15.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.8F)
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .fillMaxHeight(0.8F)
            ) {
                Text(
                    text = resources.getString("key_get_message"),
                    style = MaterialTheme.typography.titleLarge.copy(
                        textAlign = TextAlign.Center,
                    ),
                    color = if (isKeySet) Color(0xFF94FF67) else Color(0xFFFF4B4B),
                    modifier = Modifier
                        .fillMaxWidth()
                )

                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.userAgentString = USER_AGENT

                            webViewClient = client
                            webChromeClient = chromeClient
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                with(localDensity) { 350.dp.toPx().roundToInt() },
                            )

                            // Disable clicks
                            setOnTouchListener { _, _ -> true }
                            webView = this
                        }
                    },
                    update = { webView ->
                        webView.loadUrl(SHAWSHANK_REDEMPTION_WATCH_LINK)
                    },
                    modifier = Modifier
                        .weight(1F)
                )

                Button(
                    onClick = dismissProperly,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = resources.getString("close"),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }
        }
    }
}