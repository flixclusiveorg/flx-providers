package com.flixclusive.provider.app.trakt.core.model

import com.flixclusive.model.provider.Catalog
import com.flixclusive.provider.app.trakt.BuildConfig
import com.flixclusive.provider.app.trakt.core.config.TraktApiConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TraktSmartList(
    @SerialName("ids") val idMap: TraktGenericIdMap,
    @SerialName("name") val name: String,
    @SerialName("updated_at") val updatedAt: String,
) {
    fun toCatalog(
        providerId: String,
        authToken: String,
    ): Catalog {
        val urlQuery = "people_operator=and&people_role=any&extended=full%2Cimages"
        val url = "${TraktApiConfig.API_BASE_URL}/smart-lists/${idMap.id}/items/all/rank/desc?$urlQuery"
        return Catalog(
            name = name,
            description = null,
            providerId = providerId,
            url = url,
            canPaginate = true,
            headers =
                mapOf(
                    "authorization" to "Bearer $authToken",
                    "trakt-api-version" to "2",
                    "trakt-api-key" to BuildConfig.TRAKT_CLIENT_ID,
                ),
        )
    }
}
