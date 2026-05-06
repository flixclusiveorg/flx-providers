package com.flixclusive.provider.app.trakt.core.model

import com.flixclusive.model.provider.Catalog
import com.flixclusive.provider.app.trakt.BuildConfig
import com.flixclusive.provider.tracker.TrackerList
import com.flixclusive.provider.app.trakt.core.config.TraktApiConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
internal data class TraktList(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String?,
    @SerialName("privacy") val privacy: String,
    @SerialName("ids") private val ids: Ids,
    @SerialName("item_count") val itemCount: Int,
    @SerialName("share_link") val url: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("sort_by") val sortBy: String? = null,
    @SerialName("sort_how") val sortHow: String? = null,
    @SerialName("user") val user: TraktUser? = null,
    @SerialName("images") val images: Map<String, List<String>>? = null,
) {
    val slug: String get() = ids.slug
    val traktId: Int get() = ids.trakt

    @Serializable
    internal data class Ids(
        @SerialName("slug")
        val slug: String,
        @SerialName("trakt")
        val trakt: Int
    )

    fun toTrackerList(providerId: String): TrackerList {
        val imageList = images?.get("posters") ?: images?.get("thumbs")
        val images = imageList?.map { "https://$it" } ?: emptyList()
        return TrackerList(
            id = traktId.toString(),
            providerId = providerId,
            name = name,
            description = description,
            itemCount = itemCount,
            url = url,
            createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
            updatedAt = Instant.parse(updatedAt).toEpochMilliseconds(),
            images = images
        )
    }

    fun toCatalog(
        providerId: String,
        authToken: String
    ): Catalog {
        if (user == null) {
            error("User information is required to create a Catalog URL")
        }

        return Catalog(
            name = name,
            url = buildString {
                append("${TraktApiConfig.API_BASE_URL}/users/${user.traktSlug}/lists/$slug/items")
                append("?extended=full%2Cimages")
                if (sortBy != null) {
                    append("&sort_by=$sortBy")
                }

                if (sortHow != null) {
                    append("&sort_how=$sortHow")
                }
            },
            providerId = providerId,
            canPaginate = true,
            image = user.avatar,
            description = description,
            headers = buildMap {
                put("authorization", "Bearer $authToken")
                put("trakt-api-version", "2")
                put("trakt-api-key", BuildConfig.TRAKT_CLIENT_ID)
            }
        )
    }
}