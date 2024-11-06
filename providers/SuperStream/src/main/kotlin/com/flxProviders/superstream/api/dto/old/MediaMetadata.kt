package com.flxProviders.superstream.api.dto.old

import com.flixclusive.model.film.FilmDetails
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flxProviders.superstream.api.util.old.Constants.PROVIDER_TAG
import com.flxProviders.superstream.api.util.old.SuperStreamUtil.toValidReleaseDate
import com.google.gson.annotations.SerializedName

internal data class MediaMetadata(
    val id: Int? = null,
    val title: String? = null,
    val year: Int? = null,
    val released: String? = null,
    @SerializedName("max_season") val maxSeason: Int? = null,
    @SerializedName("max_episode") val maxEpisode: Int? = null,
) {
    companion object {
        fun MediaMetadata.toFilmDetails(): FilmDetails {
            return if (maxSeason == null && maxEpisode == null) {
                Movie(
                    id = id.toString(),
                    title = title
                        ?: throw NullPointerException("Movie title should not be blank or null!"),
                    releaseDate = released!!.toValidReleaseDate()!!,
                    posterImage = null,
                    homePage = null,
                    providerName = PROVIDER_TAG
                )
            } else {
                TvShow(
                    id = id.toString(),
                    title = title
                        ?: throw NullPointerException("Movie title should not be blank or null!"),
                    year = year,
                    seasons = emptyList(),
                    totalSeasons = maxSeason!!,
                    totalEpisodes = maxEpisode!!,
                    posterImage = null,
                    homePage = null,
                    providerName = PROVIDER_TAG
                )
            }
        }
    }
}
