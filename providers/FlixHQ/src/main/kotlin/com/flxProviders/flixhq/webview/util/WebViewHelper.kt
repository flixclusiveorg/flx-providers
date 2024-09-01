package com.flxProviders.flixhq.webview.util

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
internal fun WebView.setup(
    client: WebViewClient,
    chromeClient: WebChromeClient,
    userAgent: String,
) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.userAgentString = userAgent

    webViewClient = client
    webChromeClient = chromeClient
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )
    setBackgroundColor(0x00000000)

    setOnTouchListener { _, _ -> true }
}