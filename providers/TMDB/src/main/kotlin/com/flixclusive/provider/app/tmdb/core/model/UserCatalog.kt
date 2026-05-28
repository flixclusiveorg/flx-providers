package com.flixclusive.provider.app.tmdb.core.model

import android.content.Context
import com.flixclusive.core.util.network.json.AppJson
import com.flixclusive.model.provider.Catalog
import com.flixclusive.provider.app.tmdb.core.config.TMDB_IMAGE_BASE_W200
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserCatalog(
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val category: String = "custom",
    val image: String? = null,
) {
    fun toCatalog(providerId: String): Catalog = Catalog(
        name = name,
        url = url,
        providerId = providerId,
        canPaginate = true,
        image = image,
    )
}

@Serializable
internal data class TMDBHomeCatalogDto(
    val name: String,
    val url: String,
    val image: String? = null,
    val required: Boolean = false,
)

@Serializable
internal data class TMDBHomeCatalogsDto(
    val all: List<TMDBHomeCatalogDto> = emptyList(),
    val movie: List<TMDBHomeCatalogDto> = emptyList(),
    val tv: List<TMDBHomeCatalogDto> = emptyList(),
)

@Serializable
internal data class TMDBDiscoverCatalogDto(
    val name: String,
    val url: String,
    @SerialName("poster_path") val posterPath: String? = null,
)

@Serializable
internal data class TMDBDiscoverCatalogsDto(
    val networks: List<TMDBDiscoverCatalogDto> = emptyList(),
    val companies: List<TMDBDiscoverCatalogDto> = emptyList(),
    val genres: List<TMDBDiscoverCatalogDto> = emptyList(),
    val type: List<TMDBDiscoverCatalogDto> = emptyList(),
)

internal fun TMDBHomeCatalogDto.toUserCatalog(category: String) = UserCatalog(
    name = name,
    url = url,
    enabled = true,
    category = category,
    image = image,
)

internal fun TMDBDiscoverCatalogDto.toUserCatalog(category: String) = UserCatalog(
    name = name,
    url = url,
    enabled = true,
    category = category,
    image = posterPath?.let { "$TMDB_IMAGE_BASE_W200$it" },
)

internal fun loadCatalogSeed(context: Context): List<UserCatalog> {
    val result = mutableListOf<UserCatalog>()

    val homeJson = context.assets.open("home_catalogs.json").bufferedReader().use { it.readText() }
    val homeCatalogs = AppJson.decodeFromString<TMDBHomeCatalogsDto>(homeJson)
    homeCatalogs.all.forEach { result.add(it.toUserCatalog("all")) }
    homeCatalogs.movie.forEach { result.add(it.toUserCatalog("movie")) }
    homeCatalogs.tv.forEach { result.add(it.toUserCatalog("tv")) }

    val discoverJson = context.assets.open("discover_catalogs.json").bufferedReader().use { it.readText() }
    val discoverCatalogs = AppJson.decodeFromString<TMDBDiscoverCatalogsDto>(discoverJson)
    discoverCatalogs.networks.forEach { result.add(it.toUserCatalog("network")) }
    discoverCatalogs.companies.forEach { result.add(it.toUserCatalog("studio")) }
    discoverCatalogs.genres.forEach { result.add(it.toUserCatalog("genre")) }
    discoverCatalogs.type.forEach { result.add(it.toUserCatalog("type")) }

    return result
}
