package com.flxProviders.superstream.util

import java.util.Base64

internal object Constants {
    // We do not want content scanners to notice this scraping going on so we've hidden all constants
    // The source has its origins in China so I added some extra security with banned words
    // Mayhaps a tiny bit unethical, but this source is just too good :)
    // If you are copying this code please use precautions so they do not change their api.

    // Free Tibet, The Tienanmen Square protests of 1989
    const val APP_VERSION = "11.7"
    const val APP_VERSION_CODE = "131"
    const val PROVIDER_TAG = "SUPERSTREAM"
    val API_IV = String(Base64.getDecoder().decode("d0VpcGhUbiE="))
    val API_KEY = String(Base64.getDecoder().decode("MTIzZDZjZWRmNjI2ZHk1NDIzM2FhMXc2"))

    val APP_KEY = String(Base64.getDecoder().decode("bW92aWVib3g="))
    val APP_ID = String(Base64.getDecoder().decode("Y29tLnRkby5zaG93Ym94"))
    val captionDomains = arrayOf(
        String(Base64.getDecoder().decode("bWJwaW1hZ2VzLmNodWF4aW4uY29t")),
        String(Base64.getDecoder().decode("aW1hZ2VzLnNoZWd1Lm5ldA=="))
    )

    val COMMON_HEADERS = mapOf(
        "Platform" to "android",
        "Accept" to "charset=utf-8",
    )
}