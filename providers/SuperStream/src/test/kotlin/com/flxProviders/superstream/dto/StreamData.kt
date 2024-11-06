package com.flxProviders.superstream.dto

import com.google.gson.annotations.SerializedName

internal data class StreamData(
    val seconds: Int? = null,
    val quality: List<String> = listOf(),
    val list: List<StreamItem> = listOf()
)

internal data class StreamItem(
    val path: String? = null,
    val quality: String = "UNKNOWN QUALITY",
    @SerializedName("real_quality") val realQuality: String? = null,
    val fid: Int? = null,
    val size: String = "UNKNOWN FILESIZE",
    val count: Int = -1,
    val filename: String = "UNKNOWN FILE"
)