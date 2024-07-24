package com.flxProviders.sudoflix.api.nsbx.dto


internal data class NsbxSource(
    val stream: List<StreamDto>
)

internal data class StreamDto(
    val qualities: Map<String, Quality>? = null, // for VidBinge
    val playlist: String? = null, // for NSBX
    val captions: List<Caption>,
)

internal data class Quality(
    val type: String,
    val url: String
)

internal data class Caption(
    val id: String,
    val url: String,
    val type: String,
    val language: String,
    val hasCorsRestrictions: Boolean
)