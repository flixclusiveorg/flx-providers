package com.flxProviders.flixhq.extractors.rabbitstream.dto

import com.google.gson.annotations.SerializedName

internal data class DecryptedSource(
    @SerializedName("file") val url: String,
    val type: String
)