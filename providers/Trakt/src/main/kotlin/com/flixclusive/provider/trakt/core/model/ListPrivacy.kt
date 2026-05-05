package com.flixclusive.provider.trakt.core.model

enum class ListPrivacy {
    Private,
    Public;

    companion object {
        fun fromString(value: String): ListPrivacy {
            return when (value.lowercase()) {
                "private" -> Private
                "public" -> Public
                else -> throw IllegalArgumentException("Unknown ListPrivacy value: $value")
            }
        }
    }

    override fun toString(): String {
        return when (this) {
            Private -> "private"
            Public -> "public"
        }
    }
}