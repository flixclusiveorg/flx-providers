package com.flixclusive.provider.app.stremio.core.model

import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.Catalog
import com.flixclusive.provider.app.stremio.core.util.AddonUtil.toCatalog
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
internal data class Addon(
    val id: String,
    val name: String,
    val version: String,
    val description: String? = null,
    val logo: String? = null,
    val baseUrl: String? = null,
    val resources: List<AddonResource> = emptyList(),
    val types: List<String>? = emptyList(),
    internal val catalogs: List<StremioCatalog>? = null,
    private val behaviorHints: Map<String, Boolean>? = null,
) {
    val hasCatalog: Boolean
        get() = catalogs?.isNotEmpty() == true

    val hasStream: Boolean
        get() = resources.hasResource("stream")

    val hasSubtitle: Boolean
        get() = resources.hasResource("subtitles")

    val hasMeta: Boolean
        get() = resources.hasResource("meta")

    val needsConfiguration: Boolean
        get() = behaviorHints?.get("configurationRequired") ?: false

    fun getHomeCatalogs(providerId: String): List<Catalog> {
        return catalogs?.fastMapNotNull { catalog ->
            val isNotValidHomeCatalog = catalog.extra?.fastAny {
                it.name != "skip" && it.isRequired == true
            }

            if (isNotValidHomeCatalog == true) {
                return@fastMapNotNull null
            }

            catalog.toCatalog(
                providerId = providerId,
                addonId = id,
                image = logo,
            )
        } ?: emptyList()
    }

    val searchableCatalogs: List<StremioCatalog>
        get() = catalogs?.fastMapNotNull { catalog ->
            catalog.takeIf { it.canSearch }
                ?.copy(addonId = id)
        } ?: emptyList()

    fun getStreamQuery(
        id: String,
        type: MediaType,
        isFromStremio: Boolean,
        episode: Episode?,
    ): String = when (type) {
        MediaType.SHOW -> getStreamQuery("series", id, isFromStremio, episode)
        MediaType.MOVIE -> getStreamQuery("movie", id, isFromStremio, episode)
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

    /** Returns `true` if any resource in the list matches [resourceName] (case-insensitive). */
    private fun List<AddonResource>.hasResource(resourceName: String): Boolean =
        fastAny { it.name.equals(resourceName, ignoreCase = true) }

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


/**
 * Represents a Stremio addon resource, which the API returns as either:
 *  - a plain JSON string  → [Simple]   e.g. `"stream"`
 *  - a JSON object        → [Detailed] e.g. `{"name":"stream","types":["movie"],"idPrefixes":["tt"]}`
 */
@Serializable(with = AddonResource.Serializer::class)
internal sealed interface AddonResource {

    /** The canonical resource name ("stream", "meta", "catalog", "subtitles", …). */
    val name: String

    /** A resource declared as a bare string, e.g. `"stream"`. */
    @JvmInline
    value class Simple(override val name: String) : AddonResource

    /**
     * A resource declared as an object with optional type/prefix filters.
     *
     * @property types       Media types this resource applies to (e.g. `["movie","series"]`).
     * @property idPrefixes  ID prefixes the resource handles (e.g. `["tt"]` for IMDB IDs).
     */
    data class Detailed(
        override val name: String,
        val types: List<String> = emptyList(),
        val idPrefixes: List<String> = emptyList(),
    ) : AddonResource

    object Serializer : KSerializer<AddonResource> {

        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor("AddonResource")

        override fun deserialize(decoder: Decoder): AddonResource {
            val json = (decoder as? JsonDecoder)
                ?: throw SerializationException("AddonResource can only be deserialized from JSON")

            return when (val element = json.decodeJsonElement()) {
                is JsonPrimitive -> Simple(name = element.content)

                is JsonObject -> Detailed(
                    name = element.getValue("name").jsonPrimitive.content,
                    types = element["types"]
                        ?.jsonArray
                        ?.fastMap { it.jsonPrimitive.content }
                        ?: emptyList(),
                    idPrefixes = element["idPrefixes"]
                        ?.jsonArray
                        ?.fastMap { it.jsonPrimitive.content }
                        ?: emptyList(),
                )

                else -> throw SerializationException(
                    "Unexpected JSON element for AddonResource: $element"
                )
            }
        }

        override fun serialize(encoder: Encoder, value: AddonResource) {
            val json = (encoder as? JsonEncoder)
                ?: throw SerializationException("AddonResource can only be serialized to JSON")

            val element: JsonElement = when (value) {
                is Simple -> JsonPrimitive(value.name)
                is Detailed -> buildJsonObject {
                    put("name", value.name)
                    if (value.types.isNotEmpty()) {
                        put("types", kotlinx.serialization.json.buildJsonArray {
                            value.types.fastForEach { add(JsonPrimitive(it)) }
                        })
                    }
                    if (value.idPrefixes.isNotEmpty()) {
                        put("idPrefixes", kotlinx.serialization.json.buildJsonArray {
                            value.idPrefixes.fastForEach { add(JsonPrimitive(it)) }
                        })
                    }
                }
            }

            json.encodeJsonElement(element)
        }
    }
}