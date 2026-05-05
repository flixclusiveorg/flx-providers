package com.flixclusive.provider.trakt.core.network.dto.response


import androidx.compose.ui.util.fastMap
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.Genre
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.provider.trakt.core.GenreUtil.toCatalog
import com.flixclusive.provider.trakt.core.config.TraktApiConfig
import com.flixclusive.provider.trakt.core.model.ReleaseStatus
import com.flixclusive.provider.trakt.core.model.TraktGenericIdMap
import com.flixclusive.provider.trakt.core.model.TraktMedia
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

@Serializable
internal data class TraktSearchResponseV2(
    @SerialName("found") val totalFound: Int,
    @SerialName("hits") val results: List<Hit>,
    @SerialName("page") val page: Int,
) {
    val availablePages get() = (totalFound + TraktApiConfig.PAGE_RESULTS_LIMIT - 1) / TraktApiConfig.PAGE_RESULTS_LIMIT

    val hasNextPage: Boolean get() {
        val currentPage = page + 1 // API is 0-indexed, so add 1 for human-readable page number
        val availablePages = (totalFound + TraktApiConfig.PAGE_RESULTS_LIMIT - 1) / TraktApiConfig.PAGE_RESULTS_LIMIT
        return currentPage < availablePages
    }

    fun getBestMatch(): Hit? {
        return results.maxWithOrNull(
            compareBy<Hit>
            { it.textMatchInfo.numTokensDropped == 0 } // prefer no dropped tokens
                .thenBy { it.textMatchInfo.typoPrefixScore == 0 } // prefer no typos
                .thenBy { it.textMatchInfo.tokensMatched } // prefer more tokens matched
                .thenBy { it.textMatchInfo.fieldsMatched } // prefer more fields matched
                .thenBy { it.textMatchInfo.bestFieldWeight } // prefer higher-weight field
                .thenBy { it.textMatchInfo.score.toBigInteger() } // final tiebreaker
        )
    }

}

@Serializable
internal data class Hit(
    @SerialName("collection") val collection: String,
    @SerialName("document") val media: MediaDocument,
    @SerialName("text_match_info") val textMatchInfo: TextMatchInfo
) {
    fun toTraktMedia(): TraktMedia {
        return TraktMedia(
            ids = TraktGenericIdMap(
                trakt = media.id.toIntOrNull(),
                slug = media.slug,
                tmdb = media.tmdbId,
                imdb = media.imdbId,
            ),
            title = media.title,
            certification = media.certification,
            status = media.status?.let { ReleaseStatus.fromTraktStatus(it) },
            homepage = media.homepage,
            language = media.defaultLanguage,
            overview = media.overview,
            rating = media.rating,
            tagline = media.tagline?.takeIf { it.isNotBlank() },
            runtime = media.runtime,
            released = media.releaseDate,
            subgenres = media.subGenreSlugs,
            genres = media.genreSlugs,
            network = media.network,
            images = mapOf(
                "poster" to listOf("https://walter-r2.trakt.tv${media.posterUrl}"),
                "thumb" to listOf("https://walter-r2.trakt.tv${media.fanartUrl}")
            ),
            totalEpisodes = media.episodeCount?.takeIf { it > 0 }
        )
    }

    fun toPartialMedia(providerId: String): PartialMedia {

        return with(media) {
            val dateAsLong = if (releaseDate?.endsWith("Z") == true) {
                Instant.parse(releaseDate!!).toEpochMilliseconds()
            } else if (releaseDate != null) {
                Instant.parse("${releaseDate}T00:00:00Z").toEpochMilliseconds()
            } else null

            PartialMedia(
                id = id,
                title = title,
                posterImage = "https://walter-r2.trakt.tv${media.posterUrl}",
                homePage = homepage,
                backdropImage = "https://walter-r2.trakt.tv${media.fanartUrl}",
                externalIds = TraktGenericIdMap(
                    trakt = id.toIntOrNull(),
                    slug = slug,
                    tmdb = tmdbId,
                    imdb = imdbId,
                ).asMap(),
                language = defaultLanguage,
                rating = rating,
                providerId = providerId,
                overview = overview,
                certification = certification,
                releaseDate = dateAsLong,
                type = if (collection.contains("Movie", ignoreCase = true)) MediaType.MOVIE else MediaType.SHOW,
                genres = genreSlugs?.fastMap { genreName ->
                    Genre(
                        name = genreName.replaceFirstChar { it.titlecase() },
                        catalog = genreName.toCatalog(providerId)
                    )
                } ?: emptyList()
            )
        }
    }


}

@Serializable
internal data class MediaDocument(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("certification") val certification: String? = null,
    @SerialName("default_language") val defaultLanguage: String? = null,
    @SerialName("fanart_url") val fanartUrl: String? = null,
    @SerialName("first_aired") val firstAired: Long? = null,
    @SerialName("genre_slugs") val genreSlugs: List<String>? = null,
    @SerialName("subgenre_slugs") val subGenreSlugs: List<String>? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("imdb_rating") val imdbRating: Double? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("people") val people: List<String>? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("homepage") val homepage: String? = null,
    @SerialName("rating") val rating: Double? = null,
    @SerialName("runtime") val runtime: Int? = null,
    @SerialName("slug") val slug: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("tagline") val tagline: String? = null,
    @SerialName("tmdb_id") val tmdbId: Int? = null,
    @SerialName("episode_count") val episodeCount: Int? = null,
    @SerialName("network") val network: String? = null,
    @SerialName("season_numbers") val seasonNumbers: List<Int>? = null,
) {
    val releaseDate: String? get() {
        if (firstAired == null || firstAired <= 0) return null
        
        val instant = java.time.Instant.ofEpochSecond(firstAired)
        return DateTimeFormatter.ISO_INSTANT.format(instant)
    }

}

@Serializable
internal data class TextMatchInfo(
    @SerialName("best_field_score") val bestFieldScore: String,
    @SerialName("best_field_weight") val bestFieldWeight: Int,
    @SerialName("fields_matched") val fieldsMatched: Int,
    @SerialName("num_tokens_dropped") val numTokensDropped: Int,
    @SerialName("score") val score: String,
    @SerialName("tokens_matched") val tokensMatched: Int,
    @SerialName("typo_prefix_score") val typoPrefixScore: Int
)