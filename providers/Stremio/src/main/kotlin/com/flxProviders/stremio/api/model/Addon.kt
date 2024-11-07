package com.flxProviders.stremio.api.model

import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMapNotNull
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.provider.ProviderCatalog
import com.flxProviders.stremio.settings.util.AddonUtil.DEFAULT_META_PROVIDER
import com.flxProviders.stremio.settings.util.AddonUtil.toProviderCatalog

internal data class Addon(
    val id: String,
    val name: String,
    val version: String,
    val description: String? = null,
    val logo: String? = null,
    val baseUrl: String? = null,
    val resources: List<*> = listOf<Any>(),
    val types: List<String>? = emptyList(),
    internal val catalogs: List<Catalog>? = null,
    private val behaviorHints: Map<String, Boolean>? = null
) {
    val hasCatalog: Boolean
        get() = catalogs?.isNotEmpty() == true

    val hasStream: Boolean
        get() = resources.has("stream")

    val hasSubtitle: Boolean
        get() = resources.has("subtitles")

    val hasMeta: Boolean
        get() = resources.has("meta")

    val needsConfiguration: Boolean
        get() = behaviorHints?.get("configurationRequired") ?: false

    val homeCatalogs: List<ProviderCatalog>
        get() {
            val allCatalogs = mutableListOf<ProviderCatalog>()

            catalogs?.fastForEach forEachCatalog@ { catalog ->
                val isNotValidHomeCatalog = catalog.extra?.fastAny {
                    it.name != "skip" && it.isRequired == true
                }

                if (isNotValidHomeCatalog == true) {
                    return@forEachCatalog
                }

                allCatalogs.add(catalog.toProviderCatalog(image = logo))
            }

            return allCatalogs.toList()
        }

    val searchableCatalogs: List<Catalog>
        get() {
            val searchableCatalogs = catalogs?.fastMapNotNull { catalog ->
                val isNotSearchableCatalog = catalog.extra?.fastAny {
                    it.name != "search" && it.isRequired == true
                }

                val isSearchableCatalog = catalog.extra?.fastAny { it.name == "search" }

                if (isNotSearchableCatalog == true || isSearchableCatalog == false) {
                    return@fastMapNotNull null
                }

                catalog
            } ?: emptyList()

            return listOf(
                Catalog(
                    id = "top",
                    type = "movie",
                    addonSource = DEFAULT_META_PROVIDER,
                    name = "Popular Movies",
                ),
                Catalog(
                    id = "top",
                    type = "series",
                    addonSource = DEFAULT_META_PROVIDER,
                    name = "Popular Series",
                )
            ) +  searchableCatalogs
        }

    fun getStreamQuery(
        id: String,
        type: FilmType,
        isFromStremio: Boolean,
        episode: Episode?,
    ): String {
        return when (type) {
            FilmType.TV_SHOW -> getStreamQuery(
                type = "series",
                id = id,
                isFromStremio = isFromStremio,
                episode = episode
            )
            FilmType.MOVIE -> getStreamQuery(
                type = "movie",
                id = id,
                isFromStremio = isFromStremio,
                episode = episode
            )
        }
    }

    fun getStreamQuery(
        id: String,
        type: String,
        isFromStremio: Boolean,
        episode: Episode?,
    ): String = when {
        isFromStremio && episode != null -> "stream/$type/${episode.id}.json"
        episode == null -> "stream/$type/$id.json"
        else -> "stream/$type/$id:${episode.season}:${episode.number}.json"
    }

    private fun List<*>.has(resource: String): Boolean {
        return any {
            if (it is String) {
                return@any it.equals(resource, true)
            }

            if (it is Map<*, *>) {
                return@any safeCall {
                    (it["name"] as String).equals(resource, true)
                } ?: false
            }

            false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is Addon) {
            return baseUrl != null && baseUrl == other.baseUrl
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + (logo?.hashCode() ?: 0)
        result = 31 * result + (baseUrl?.hashCode() ?: 0)
        result = 31 * result + catalogs.hashCode()
        result = 31 * result + resources.hashCode()
        result = 31 * result + types.hashCode()
        result = 31 * result + (behaviorHints?.hashCode() ?: 0)
        result = 31 * result + hasCatalog.hashCode()
        result = 31 * result + hasStream.hashCode()
        result = 31 * result + hasMeta.hashCode()
        result = 31 * result + needsConfiguration.hashCode()
        return result
    }
}