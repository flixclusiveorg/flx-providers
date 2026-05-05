package com.flixclusive.provider.trakt.core.network.dto.response

import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.provider.trakt.core.model.TraktMedia
import com.flixclusive.provider.trakt.core.model.TraktMedia.Companion.toPartialMedia
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlin.time.Instant

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = TraktGenericMediaItemResponseSerializer::class)
internal data class TraktGenericMediaItemResponse(
    @JsonNames("movie", "show") val media: TraktMedia,
    @SerialName("id") val id: Long? = null,
    @JsonNames("listed_at", "watched_at", "last_updated_at") val listedAt: String? = null,
    @SerialName("type") val type: String?
) {
    val listedAtAsLong get() = listedAt?.let {
        if (it.endsWith("Z")) {
            Instant.parse(it).toEpochMilliseconds()
        } else {
            Instant.parse("${it}T00:00:00Z").toEpochMilliseconds()
        }
    }

    val filmType
        get() = when (type) {
            "movie" -> MediaType.MOVIE
            "show" -> MediaType.SHOW
            else -> throw IllegalArgumentException("Unknown trakt media type: $type")
        }

    fun toPartialMedia(providerId: String): PartialMedia {
        return media.toPartialMedia(providerId)
    }
}

internal object TraktGenericMediaItemResponseSerializer :
    KSerializer<TraktGenericMediaItemResponse> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("TraktGenericMediaItemResponse") {
            element<TraktMedia>("media")
            element<Int?>("id", isOptional = true)
            element<String?>("type", isOptional = true)
        }

    override fun deserialize(decoder: Decoder): TraktGenericMediaItemResponse {
        val json = decoder as JsonDecoder
        val element = json.decodeJsonElement().jsonObject

        // Determine type from whichever key is present
        val (mediaKey, mediaElement) = when {
            "movie" in element -> "movie" to element["movie"]!!
            "show" in element -> "show" to element["show"]!!
            else -> throw SerializationException("No 'movie' or 'show' key found")
        }

        val media = json.json.decodeFromJsonElement<TraktMedia>(mediaElement)
        val id = element["id"]?.jsonPrimitive?.longOrNull
        val listedAt = (element["listed_at"]
            ?: element["watched_at"]
            ?: element["collected_at"])?.jsonPrimitive
            ?.contentOrNull

        return TraktGenericMediaItemResponse(
            media = media,
            type = mediaKey,
            listedAt = listedAt,
            id = id
        )
    }

    override fun serialize(encoder: Encoder, value: TraktGenericMediaItemResponse) {
        val json = encoder as JsonEncoder
        val mediaKey = value.type ?: "movie" // fall back to a default if type is null

        val obj = buildJsonObject {
            put(mediaKey, json.json.encodeToJsonElement(value.media))
            value.id?.let { put("id", it) }
            put("type", mediaKey)
        }

        json.encodeJsonElement(obj)
    }
}