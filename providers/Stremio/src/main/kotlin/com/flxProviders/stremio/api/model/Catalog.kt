package com.flxProviders.stremio.api.model

import com.google.gson.annotations.SerializedName

internal data class Catalog(
    val id: String,
    val name: String,
    val type: String,
    val addonSource: String = "",
    val pageSize: Int? = null,
    private val extra: List<CatalogExtraOptions>? = null
) {
    val canPaginate: Boolean
        get() = extra?.any {
            it.name.equals("skip", true)
        } ?: false
}

internal data class CatalogExtraOptions(
    val name: String,
    val options: List<String>? = null,
)

internal data class FetchCatalogResponse(
    @SerializedName("metas") val items: List<Meta>? = null,
    override val err: String?,
) : CommonErrorResponse()