package com.flixclusive.provider.trakt.core.config

internal interface TypeSenseKeyProvider {
    val typeSenseKey: String?

    suspend fun reloadTypeSenseKey()
}