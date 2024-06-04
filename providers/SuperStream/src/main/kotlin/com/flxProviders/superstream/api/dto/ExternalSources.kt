package com.flxProviders.superstream.api.dto

import com.google.gson.annotations.SerializedName

data class ExternalSources(
    @SerializedName("m3u8_url") val hlsUrl: String? = null,
    val file: String? = null,
    val label: String? = null,
    val type: String? = null,
)