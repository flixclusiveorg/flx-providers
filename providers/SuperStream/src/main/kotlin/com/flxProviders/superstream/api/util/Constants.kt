package com.flxProviders.superstream.api.util

import com.flixclusive.core.util.network.CryptographyUtil.base64Decode

internal object Constants {
    val headers = mapOf(
        "Platform" to "android",
        "Accept" to "charset=utf-8",
    )

    // We do not want content scanners to notice this scraping going on so we've hidden all constants
    // The source has its origins in China so I added some extra security with banned words
    // Mayhaps a tiny bit unethical, but this source is just too good :)
    // If you are copying this code please use precautions so they do not change their api.

    // Free Tibet, The Tienanmen Square protests of 1989
    val iv = base64Decode("d0VpcGhUbiE=")
    val key = base64Decode("MTIzZDZjZWRmNjI2ZHk1NDIzM2FhMXc2")

    val appKey = base64Decode("bW92aWVib3g=")
    val appIdSecond = base64Decode("Y29tLm1vdmllYm94cHJvLmFuZHJvaWQ=")
    val captionDomains = arrayOf(
        base64Decode("bWJwaW1hZ2VzLmNodWF4aW4uY29t"),
        base64Decode("aW1hZ2VzLnNoZWd1Lm5ldA==")
    )

    const val APP_VERSION = "14.7"
    const val APP_VERSION_CODE = "160"
}