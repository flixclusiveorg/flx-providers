package com.flixclusive.provider.trakt.core.network.dto.request

import com.flixclusive.provider.trakt.core.model.ListPrivacy
import kotlinx.serialization.Serializable

@Serializable
internal data class ListCreateRequest(
    val name: String,
    val privacy: String = ListPrivacy.Public.toString(),
    val description: String = ""
)