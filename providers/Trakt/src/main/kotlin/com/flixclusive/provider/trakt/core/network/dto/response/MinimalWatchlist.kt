package com.flixclusive.provider.trakt.core.network.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MinimalWatchlist(
    @SerialName("movies") val movies: List<Int> = emptyList(),
    @SerialName("shows") val shows: List<Int> = emptyList(),
) {
    fun isInList(id: Int): Boolean {
        return movies.contains(id) || shows.contains(id)
    }
}