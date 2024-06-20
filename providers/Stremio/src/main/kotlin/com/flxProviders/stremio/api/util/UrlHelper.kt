package com.flxProviders.stremio.api.util

import com.flixclusive.core.util.exception.safeCall
import java.net.URL

internal fun isValidUrl(url: String?)
    = safeCall {
        URL(url)
        return@safeCall true
    } ?: false