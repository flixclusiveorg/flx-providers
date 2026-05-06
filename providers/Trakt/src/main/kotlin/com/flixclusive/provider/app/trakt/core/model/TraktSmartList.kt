package com.flixclusive.provider.app.trakt.core.model


import com.flixclusive.model.provider.Catalog
import com.flixclusive.provider.app.trakt.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TraktSmartList(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("path") val path: String,
    @SerialName("query") val query: String,
    @SerialName("rank") val rank: Int,
    @SerialName("section") val section: String,
    @SerialName("updated_at") val updatedAt: String
) {
    fun toCatalog(
        providerId: String,
        authToken: String,
    ): Catalog {
        val url = "https://apiz.trakt.tv${path}?extended=full%2Cimages&${query}"
        return Catalog(
            name = name,
            description = null,
            providerId = providerId,
            url = url,
            canPaginate = true,
            headers = mapOf(
                "authorization" to "Bearer $authToken",
                "trakt-api-version" to "2",
                "trakt-api-key" to BuildConfig.TRAKT_CLIENT_ID
            )
        )
    }
}