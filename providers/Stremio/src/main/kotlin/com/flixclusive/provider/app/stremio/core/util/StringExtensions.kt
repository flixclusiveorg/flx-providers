package com.flixclusive.provider.app.stremio.core.util

import kotlin.time.Instant


fun String.toMs(): Long? {
    try {
        if (contains("T")) {
            return Instant.parse(this).toEpochMilliseconds()
        }

        val modifiedDate = "${this}T00:00:00Z"
        return Instant.parse(modifiedDate).toEpochMilliseconds()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}