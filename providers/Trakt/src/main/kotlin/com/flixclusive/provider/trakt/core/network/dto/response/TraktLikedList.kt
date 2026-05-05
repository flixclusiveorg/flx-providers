package com.flixclusive.provider.trakt.core.network.dto.response

import com.flixclusive.provider.trakt.core.model.TraktList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TraktLikedList(
    @SerialName("list") val list: TraktList,
//    @SerialName("liked_at") val likedAt: String,
//    @SerialName("type") val type: String
)