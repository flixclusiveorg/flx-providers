package com.flixclusive.provider.app.stremio.core.model

import androidx.compose.ui.util.fastAny
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class StremioCatalog(
    val id: String,
    val name: String,
    val type: String,
    val addonId: String = "",
    val pageSize: Int? = null,
    private val filter: Pair<String, String>? = null,
    internal val extra: List<CatalogExtraOptions>? = null
) {
    val canPaginate: Boolean
        get() = extra?.fastAny {
            it.name.equals("skip", true)
        } ?: false

    val canSearch: Boolean
        get() = extra?.fastAny {
            it.name.equals("search", true)
        } ?: false

    fun getCatalogQuery(
        page: Int = 1,
        searchQuery: String? = null,
    ): String {
        val path = "catalog/$type/$id"

        val additionalQuery = mutableListOf<String>()
        if (filter != null) {
            val (name, value) = filter
            additionalQuery.add("$name=$value")
        }

        if (page > 1 && canPaginate) {
            additionalQuery.add("skip=${page * (pageSize ?: 20)}")
        }

        if (searchQuery != null) {
            additionalQuery.add("search=$searchQuery")
        }

        val finalQuery = if (additionalQuery.isNotEmpty()) {
            "/" + additionalQuery.joinToString("&")
        } else ""

        return "$path${finalQuery}.json"
    }
}

@Serializable
internal data class CatalogExtraOptions(
    val name: String,
    val isRequired: Boolean? = null,
    val options: List<String?>? = null,
)

@Serializable
internal data class FetchCatalogResponse(
    @SerialName("metas") val items: List<Meta>? = null,
    override val err: String? = null,
) : CommonErrorResponse()