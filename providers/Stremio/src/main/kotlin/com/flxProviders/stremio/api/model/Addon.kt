package com.flxProviders.stremio.api.model

import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.model.tmdb.common.tv.Episode

internal data class Addon(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val logo: String? = null,
    val baseUrl: String? = null,
    val catalogs: List<Catalog> = emptyList(),
    val resources: List<*> = listOf<Any>(),
    val types: List<String> = emptyList(),
    private val behaviorHints: Map<String, Boolean>? = null
) {
    val hasCatalog: Boolean
        get() = catalogs.isNotEmpty()

    val hasStream: Boolean
        get() = resources.has("stream")

    val hasMeta: Boolean
        get() = resources.has("meta")

    val needsConfiguration: Boolean
        get() = behaviorHints?.get("configurationRequired") ?: false

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