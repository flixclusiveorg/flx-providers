package com.flixclusive.provider.app.trakt.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TraktUserWrapper(
    @SerialName("user") val user: TraktUser
) {
    val traktSlug: String get() = user.traktSlug

}

@Serializable
internal data class TraktUser(
    val deleted: Boolean,
    val name: String,
    val username: String,
    val about: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    private val ids: UserId,
    private val images: Map<String, Map<String, String>>,
    val email: String? = null,
) {
    val traktSlug: String get() = ids.slug

    val avatar: String? get() = images["avatar"]?.get("full")

    @Serializable
    data class UserId(
        @SerialName("slug") val slug: String,
    )
}
