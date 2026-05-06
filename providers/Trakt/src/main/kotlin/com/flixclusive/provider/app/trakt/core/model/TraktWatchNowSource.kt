package com.flixclusive.provider.app.trakt.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TraktWatchNowSource(
    @SerialName("source") val id: String,
    @SerialName("name") val name: String,
    @SerialName("images") private val images: Map<String, String?>,
//    @SerialName("amazon") val amazon: Boolean,
//    @SerialName("cinema") val cinema: Boolean,
//    @SerialName("free") val free: Boolean,
) {
    val logo get() = images["logo"]
}

