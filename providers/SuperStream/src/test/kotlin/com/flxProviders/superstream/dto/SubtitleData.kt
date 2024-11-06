package com.flxProviders.superstream.dto

import com.flxProviders.superstream.util.Constants.captionDomains
import com.google.gson.annotations.SerializedName

internal data class SubtitleItem(
    @SerializedName("file_path") val filePath: String? = null,
    val lang: String? = null,
    val language: String? = null,
    val order: Int? = null,
) {
    companion object {
        fun String.toValidSubtitleFilePath(): String {
            return replace(captionDomains[0], captionDomains[1])
                .replace(Regex("\\s"), "+")
                .replace(Regex("[()]")) { result ->
                    "%" + result.value.toCharArray()[0].code.toByte().toString(16)
                }
        }
    }
}

internal data class SubtitleSubData(
    val language: String? = null,
    val subtitles: List<SubtitleItem> = listOf()
)

internal data class SubtitleData(
    val select: List<String> = listOf(),
    val list: List<SubtitleSubData> = listOf()
)