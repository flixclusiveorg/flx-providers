package com.flixclusive.provider.trakt.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ReleaseStatusSerializer::class)
enum class ReleaseStatus {
    RELEASED,
    PLANNED,
    POST_PRODUCTION,
    CANCELED,
    IN_PRODUCTION,
    RUMORED,
    ENDED,
    RETURNING_SERIES,
    PILOT,
    CONTINUING,
    UPCOMING;

    val isFinished get() = this == RELEASED || this == CANCELED || this == ENDED

    companion object {
        fun fromTraktStatus(status: String): ReleaseStatus {
            return when (status.lowercase()) {
                "released" -> RELEASED
                "planned" -> PLANNED
                "post production" -> POST_PRODUCTION
                "canceled" -> CANCELED
                "in production" -> IN_PRODUCTION
                "rumored" -> RUMORED
                "ended" -> ENDED
                "returning series" -> RETURNING_SERIES
                "pilot" -> PILOT
                "continuing" -> CONTINUING
                "upcoming" -> UPCOMING
                else -> throw IllegalArgumentException("Unknown release status: $status")
            }
        }
    }
}

internal object ReleaseStatusSerializer : KSerializer<ReleaseStatus> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("ReleaseStatus", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ReleaseStatus {
        val status = decoder.decodeString()
        return ReleaseStatus.fromTraktStatus(status)
    }

    override fun serialize(encoder: Encoder, value: ReleaseStatus) {
        encoder.encodeString(value.name.lowercase().replace('_', ' '))
    }
}