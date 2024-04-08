package com.flxProviders.flixhq.api.dto

internal data class FlixHQInitialSourceData(
    val type: String,
    val link: String,
    val sources: List<Any>,
    val tracks: List<Any>,
    val title: String
)