package com.flixclusive.provider.app.letterboxd.core.model

import kotlinx.serialization.Serializable

@Serializable
internal data class Member(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
)
