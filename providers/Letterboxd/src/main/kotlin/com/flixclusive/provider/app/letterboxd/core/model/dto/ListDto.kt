package com.flixclusive.provider.app.letterboxd.core.model.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class MemberSummaryDto(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatar: ImageDto? = null,
)

@Serializable
internal data class MemberAccountDto(
    val member: MemberSummaryDto? = null,
)

@Serializable
internal data class ListSummaryDto(
    val id: String = "",
    val name: String = "",
    val filmCount: Int = 0,
    val description: String? = null,
    val published: Boolean = false,
    val owner: MemberSummaryDto? = null,
    val links: List<LinkDto> = emptyList(),
    val previewEntries: List<ListEntryDto> = emptyList(),
)

@Serializable
internal data class ListsResponseDto(
    val next: String? = null,
    val items: List<ListSummaryDto> = emptyList(),
)

@Serializable
internal data class ListEntryDto(
    val rank: Int? = null,
    val film: FilmDto? = null,
)

@Serializable
internal data class ListEntriesResponseDto(
    val next: String? = null,
    val items: List<ListEntryDto> = emptyList(),
)

@Serializable
internal data class LogEntryDto(
    val id: String = "",
    val name: String = "",
    val film: FilmDto? = null,
)

@Serializable
internal data class LogEntriesResponseDto(
    val next: String? = null,
    val items: List<LogEntryDto> = emptyList(),
)

/**
 * `GET /film/{id}/me`. The nested `relationship` field lets the same shape parse
 * both a bare relationship object and one wrapped in a response envelope.
 */
@Serializable
internal data class FilmRelationshipDto(
    val watched: Boolean = false,
    val liked: Boolean = false,
    val favorited: Boolean = false,
    val inWatchlist: Boolean = false,
    val rating: Double? = null,
    val relationship: FilmRelationshipDto? = null,
) {
    val effective: FilmRelationshipDto get() = relationship ?: this
}
