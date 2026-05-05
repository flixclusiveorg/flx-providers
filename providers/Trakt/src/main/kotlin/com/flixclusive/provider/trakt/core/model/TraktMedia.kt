package com.flixclusive.provider.trakt.core.model

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastSumBy
import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.Company
import com.flixclusive.model.media.common.Genre
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.provider.trakt.core.GenreUtil.toCatalog
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlin.time.Instant

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class TraktMedia(
    @SerialName("ids") private val ids: TraktGenericIdMap,
    @SerialName("title") val title: String,
    @SerialName("certification") val certification: String?,
    @Serializable(with = ReleaseStatusSerializer::class)
    @SerialName("status") val status: ReleaseStatus?,
    @SerialName("homepage") val homepage: String?,
    @SerialName("language") val language: String?,
    @SerialName("overview") val overview: String?,
    @SerialName("rating") val rating: Double?,
    @SerialName("tagline") val tagline: String?,
    @SerialName("runtime") val runtime: Int?,
    @JsonNames("released", "first_aired") val released: String?,
    @SerialName("subgenres") val subgenres: List<String>?,
    @SerialName("genres") val genres: List<String>?,
    @SerialName("images") val images: Map<String, List<String>>?,
    @SerialName("network") val network: String? = null,
    @SerialName("aired_episodes") val totalEpisodes: Int? = null,
) {
    val id get() = ids.id
    val logo get() = images?.get("logo")?.firstOrNull()?.let {
        if (it.startsWith("http")) it else "https://$it"
    }
    val poster get() = images?.get("poster")?.firstOrNull()?.let {
        if (it.startsWith("http")) it else "https://$it"
    }
    val backdrop get() = (
        images?.get("fanart")
            ?: images?.get("banner")
            ?: images?.get("thumb")
    )?.firstOrNull()?.let {
        if (it.startsWith("http")) it else "https://$it"
    }

    val dateAsLong get() = released?.let {
        if (it.endsWith("Z")) {
            Instant.parse(it).toEpochMilliseconds()
        } else {
            Instant.parse("${it}T00:00:00Z").toEpochMilliseconds()
        }
    }

    val externalIds get() = ids.asMap()

    companion object {
        fun TraktMedia.toMovie(providerId: String): Movie {
            return Movie(
                id = id,
                title = title,
                posterImage = poster,
                homePage = homepage,
                backdropImage = backdrop,
                logoImage = logo,
                externalIds = externalIds,
                language = language,
                releaseDate = dateAsLong,
                rating = rating,
                providerId = providerId,
                runtime = runtime,
                overview = overview,
                tagLine = tagline,
                certification = certification,
                genres = genres?.fastMap { genreName ->
                    Genre(
                        name = genreName.replaceFirstChar { it.titlecase() },
                        catalog = genreName.toCatalog(providerId)
                    )
                } ?: emptyList(),
            )
        }

        fun TraktMedia.toShow(
            providerId: String,
            seasons: List<Season>,
        ): Show {
            val network = network?.let { Company(name = it) }
            val lastAirDate = if (status == ReleaseStatus.ENDED) {
                seasons.lastOrNull()
                    ?.episodes?.lastOrNull()
                        ?.releaseDate
            } else {
                null
            }

            return Show(
                id = id,
                title = title,
                posterImage = poster,
                homePage = homepage,
                backdropImage = backdrop,
                logoImage = logo,
                externalIds = externalIds,
                language = language,
                rating = rating,
                providerId = providerId,
                runtime = runtime,
                overview = overview,
                tagLine = tagline,
                certification = certification,
                networks = listOfNotNull(network),
                releaseDate = dateAsLong,
                lastAirDate = lastAirDate,
                totalSeasons = seasons.size,
                totalEpisodes = totalEpisodes ?: seasons.fastSumBy { it.episodes.size },
                seasons = seasons,
                genres = genres?.fastMap { genreName ->
                    Genre(
                        name = genreName.replaceFirstChar { it.titlecase() },
                        catalog = genreName.toCatalog(providerId)
                    )
                } ?: emptyList()
            )
        }

        fun TraktMedia.toPartialMedia(providerId: String): PartialMedia {
            return PartialMedia(
                id = id,
                title = title,
                posterImage = poster,
                homePage = homepage,
                backdropImage = backdrop,
                logoImage = logo,
                externalIds = externalIds,
                language = language,
                rating = rating,
                providerId = providerId,
                overview = overview,
                certification = certification,
                releaseDate = dateAsLong,
                type = if (totalEpisodes != null) MediaType.SHOW else MediaType.MOVIE,
                genres = genres?.fastMap { genreName ->
                    Genre(
                        name = genreName.replaceFirstChar { it.titlecase() },
                        catalog = genreName.toCatalog(providerId)
                    )
                } ?: emptyList()
            )
        }
    }
}