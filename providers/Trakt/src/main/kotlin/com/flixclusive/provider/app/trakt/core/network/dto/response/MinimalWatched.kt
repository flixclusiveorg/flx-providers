package com.flixclusive.provider.app.trakt.core.network.dto.response

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = MinimalWatchedItemSerializer::class)
internal sealed class MinimalWatchedItem {
    abstract val id: String

    data class Movie(
        override val id: String,
        val timestamps: List<String>
    ) : MinimalWatchedItem()

    data class Show(
        override val id: String,
        val seasons: Map<String, Map<String, List<String>>>
    ) : MinimalWatchedItem()
}

internal object MinimalWatchedItemSerializer : KSerializer<MinimalWatchedItem> {
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("MinimalWatchedItem", SerialKind.CONTEXTUAL)

    override fun deserialize(decoder: Decoder): MinimalWatchedItem {
        val jsonDecoder = decoder as JsonDecoder
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> MinimalWatchedItem.Movie(
                id = "",
                timestamps = element.fastMap { it.jsonPrimitive.content }
            )

            is JsonObject -> MinimalWatchedItem.Show(
                id = "",
                seasons = element.mapValues { (_, season) ->
                    season.jsonObject.mapValues { (_, episodes) ->
                        episodes.jsonArray.fastMap { it.jsonPrimitive.content }
                    }
                }
            )

            else -> throw SerializationException("Unexpected element type")
        }
    }

    override fun serialize(encoder: Encoder, value: MinimalWatchedItem) {
        val jsonEncoder = encoder as JsonEncoder
        jsonEncoder.encodeJsonElement(
            when (value) {
                is MinimalWatchedItem.Movie -> buildJsonArray {
                    value.timestamps.fastForEach { add(it) }
                }

                is MinimalWatchedItem.Show -> buildJsonObject {
                    value.seasons.forEach { (seasonKey, episodes) ->
                        put(seasonKey, buildJsonObject {
                            episodes.forEach { (episodeId, timestamps) ->
                                put(episodeId, buildJsonArray { timestamps.forEach { add(it) } })
                            }
                        })
                    }
                }
            }
        )
    }
}

@Serializable(with = MinimalWatchedItemMapSerializer::class)
internal class MinimalWatchedItemMap(private val items: Map<String, MinimalWatchedItem>) :
    Map<String, MinimalWatchedItem> by items {

    fun isWatched(traktId: String): Boolean {
        return items.containsKey(traktId)
    }

    fun isEpisodeWatched(showId: String, seasonNumber: Int, episodeId: String): Boolean {
        val show = items[showId] as? MinimalWatchedItem.Show ?: return false

        val episodes = show.seasons.entries
            .firstOrNull { (key, _) -> key.substringAfterLast('|') == seasonNumber.toString() }
            ?.value
            ?: return false

        return episodes.containsKey(episodeId)
    }
}

internal object MinimalWatchedItemMapSerializer : KSerializer<MinimalWatchedItemMap> {
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("MinimalWatchedItemMap", SerialKind.CONTEXTUAL)

    override fun deserialize(decoder: Decoder): MinimalWatchedItemMap {
        val jsonDecoder = decoder as JsonDecoder
        return MinimalWatchedItemMap(
            jsonDecoder.decodeJsonElement().jsonObject.entries.associate { (id, element) ->
                id to when (element) {
                    is JsonArray -> MinimalWatchedItem.Movie(
                        id = id,
                        timestamps = element.fastMap { it.jsonPrimitive.content }
                    )

                    is JsonObject -> MinimalWatchedItem.Show(
                        id = id,
                        seasons = element.mapValues { (_, season) ->
                            season.jsonObject.mapValues { (_, episodes) ->
                                episodes.jsonArray.fastMap { it.jsonPrimitive.content }
                            }
                        }
                    )

                    else -> throw SerializationException("Unexpected element type for id=$id")
                }
            }
        )
    }

    override fun serialize(encoder: Encoder, value: MinimalWatchedItemMap) {
        val jsonEncoder = encoder as JsonEncoder
        jsonEncoder.encodeJsonElement(buildJsonObject {
            value.forEach { (_, item) ->
                put(
                    item.id, when (item) {
                    is MinimalWatchedItem.Movie -> buildJsonArray {
                        item.timestamps.fastForEach { add(it) }
                    }

                    is MinimalWatchedItem.Show -> buildJsonObject {
                        item.seasons.forEach { (seasonKey, episodes) ->
                            put(seasonKey, buildJsonObject {
                                episodes.forEach { (episodeId, timestamps) ->
                                    put(
                                        episodeId,
                                        buildJsonArray { timestamps.fastForEach { add(it) } })
                                }
                            })
                        }
                    }
                })
            }
        })
    }
}