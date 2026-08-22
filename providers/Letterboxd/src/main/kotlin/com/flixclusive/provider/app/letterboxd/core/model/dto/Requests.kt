package com.flixclusive.provider.app.letterboxd.core.model.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class TokenResponseDto(
    val access_token: String = "",
    val refresh_token: String = "",
    val token_type: String = "Bearer",
    val expires_in: Long = 0L,
)

/** The `{error, error_description}` body Letterboxd returns from `/auth/token`. */
@Serializable
internal data class TokenErrorDto(
    val error: String = "",
    val error_description: String = "",
)

@Serializable
internal data class WatchlistUpdateRequest(
    val watchlisted: Boolean,
)

@Serializable
internal data class WatchUpdateRequest(
    val watched: Boolean,
)

@Serializable
internal data class DiaryDetailsRequest(
    val diaryDate: String,
    val rewatch: Boolean = false,
)

@Serializable
internal data class LogEntryCreationRequest(
    val filmId: String,
    val diaryDetails: DiaryDetailsRequest? = null,
    val rating: Double? = null,
)

@Serializable
internal data class ListCreationRequest(
    val name: String,
    val description: String? = null,
    val published: Boolean = true,
)

@Serializable
internal data class ListUpdateRequest(
    val name: String? = null,
    val description: String? = null,
)
