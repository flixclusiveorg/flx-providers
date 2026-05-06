package com.flixclusive.provider.app.trakt.core.config

internal interface TypeSenseKeyProvider {
    val typeSenseKey: String?

    suspend fun reloadTypeSenseKey()
}