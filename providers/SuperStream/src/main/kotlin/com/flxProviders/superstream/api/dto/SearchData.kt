package com.flxProviders.superstream.api.dto

import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.SearchResponseData
import com.flxProviders.superstream.BuildConfig.SUPERSTREAM_FIRST_API
import com.flxProviders.superstream.BuildConfig.SUPERSTREAM_FOURTH_API
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

internal const val ITEMS_PER_PAGE = 20


internal fun Long.toStringDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = Date(this * 1000) // Convert seconds to milliseconds
    return sdf.format(date)
}

internal data class SearchData(
    val data: Data,
) {
    data class Data(
        val numFound: Int,
        val start: Int,
        @SerializedName("docs") val results: List<Item>
    )

    data class Item(
        @SerializedName("id") private val rawId: String,
        @SerializedName("imdb") val imdbId: String? = null,
        @SerializedName("released_timestamp") private val releasedTimestamp: String? = null,
        @SerializedName("audio_lang") val language: String? = null,
        @SerializedName("imdb_rating") private val imdbRating: String? = null,
        private val poster: String? = null,
        val name: String,
        val year: String? = null,
    ) {
        val rating: Double
            get() = imdbRating?.toDoubleOrNull() ?: 0.0

        val id: String?
            get() = rawId.split("_").lastOrNull()

        val releaseDate: String?
            get() = safeCall {
                releasedTimestamp
                    ?.toLongOrNull()
                    ?.toStringDate()
            }

        val filmType: FilmType
            get() = when (rawId.split("_").firstOrNull()?.lowercase()) {
                "tv" -> FilmType.TV_SHOW
                "movie" -> FilmType.MOVIE
                else -> throw IllegalArgumentException("[SuperStream]> Unknown film type")
            }

        val homePage: String
            get() = "$SUPERSTREAM_FIRST_API/${filmType.type}/$id"

        val posterImage: String?
            get() = poster?.let { SUPERSTREAM_FOURTH_API + it }
    }

    companion object {
        fun SearchData.toSearchResponseData(provider: String): SearchResponseData<FilmSearchItem> {
            val itemsLoaded = data.start + ITEMS_PER_PAGE
            val currentPage = itemsLoaded / ITEMS_PER_PAGE
            val hasNextPage = itemsLoaded < data.numFound

            return SearchResponseData(
                page = currentPage,
                hasNextPage = hasNextPage,
                results = data.results.map { it.toFilmSearchItem(provider) },
                totalPages = data.numFound.toDouble().div(ITEMS_PER_PAGE).roundToInt()
            )
        }

        private fun Item.toFilmSearchItem(provider: String)
            = FilmSearchItem(
                id = id,
                providerName = provider,
                title = name,
                imdbId = imdbId,
                rating = rating,
                releaseDate = releaseDate,
                homePage = homePage,
                posterImage = posterImage,
                filmType = filmType,
            )
    }
}