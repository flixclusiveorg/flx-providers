package com.flixclusive.provider.app.letterboxd.core.model.dto

import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.MediaType
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.time.LocalDate
import java.time.ZoneOffset

private fun image(vararg widths: Int) =
    ImageDto(sizes = widths.map { ImageSizeDto(width = it, height = it * 3 / 2, url = "https://img/$it.jpg") })

private fun epochOf(
    year: Int,
    month: Int,
    day: Int,
) = LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

class MappersTest {
    @Test
    fun `picks the rendition closest to the target width`() {
        expectThat(image(150, 500, 1000).bestUrl(600)).isEqualTo("https://img/500.jpg")
    }

    @Test
    fun `falls back to the only rendition available`() {
        expectThat(image(70).bestUrl(600)).isEqualTo("https://img/70.jpg")
    }

    @Test
    fun `a missing or empty image yields no url`() {
        expectThat(null.bestUrl(600)).isNull()
        expectThat(ImageDto().bestUrl(600)).isNull()
    }

    @Test
    fun `ignores renditions with a blank url`() {
        val dto = ImageDto(sizes = listOf(ImageSizeDto(width = 600, url = ""), ImageSizeDto(width = 100, url = "https://img/100.jpg")))

        expectThat(dto.bestUrl(600)).isEqualTo("https://img/100.jpg")
    }

    @Test
    fun `reads tmdb and imdb ids out of the links array`() {
        val dto =
            FilmDto(
                links =
                    listOf(
                        LinkDto(type = "letterboxd", id = "b8wK", url = "https://letterboxd.com/film/heat/"),
                        LinkDto(type = "tmdb", id = "949", url = "https://themoviedb.org/movie/949"),
                        LinkDto(type = "imdb", id = "tt0113277", url = "https://imdb.com/title/tt0113277"),
                    ),
            )

        val ids = dto.externalIds()

        expectThat(ids[MediaIdSource.TMDB]).isEqualTo("949")
        expectThat(ids[MediaIdSource.IMDB]).isEqualTo("tt0113277")
    }

    @Test
    fun `a film with no links has no external ids`() {
        expectThat(FilmDto().externalIds()).hasSize(0)
    }

    @Test
    fun `prefers a full release date over the year`() {
        val dto = FilmDto(releaseDate = "1995-12-15", releaseYear = 1995)

        expectThat(dto.releaseDateMs()).isEqualTo(epochOf(1995, 12, 15))
    }

    @Test
    fun `falls back to january first of the release year`() {
        expectThat(FilmDto(releaseYear = 1995).releaseDateMs()).isEqualTo(epochOf(1995, 1, 1))
    }

    @Test
    fun `an unparseable date still falls back to the year`() {
        val dto = FilmDto(releaseDate = "not-a-date", releaseYear = 1995)

        expectThat(dto.releaseDateMs()).isEqualTo(epochOf(1995, 1, 1))
    }

    @Test
    fun `a film with no date information has none`() {
        expectThat(FilmDto().releaseDateMs()).isNull()
    }

    @Test
    fun `maps a summary to a movie partial`() {
        val dto =
            FilmDto(
                id = "b8wK",
                name = "Heat",
                releaseYear = 1995,
                rating = 4.3,
                poster = image(600),
                genres = listOf(GenreDto(id = "g1", name = "Crime"), GenreDto(name = "")),
            )

        val partial = dto.toPartialMedia("letterboxd")

        expectThat(partial).isNotNull()
        expectThat(partial!!.type).isEqualTo(MediaType.MOVIE)
        expectThat(partial.id).isEqualTo("b8wK")
        expectThat(partial.title).isEqualTo("Heat")
        expectThat(partial.providerId).isEqualTo("letterboxd")
        expectThat(partial.posterImage).isEqualTo("https://img/600.jpg")
        expectThat(partial.rating).isEqualTo(4.3)
        expectThat(partial.genres.map { it.name }).isEqualTo(listOf("Crime"))
    }

    @Test
    fun `a film missing an id or name is not mappable`() {
        expectThat(FilmDto(name = "Heat").toPartialMedia("letterboxd")).isNull()
        expectThat(FilmDto(id = "b8wK").toPartialMedia("letterboxd")).isNull()
    }

    @Test
    fun `an adult film uses the unobfuscated poster when one is present`() {
        val dto = FilmDto(id = "x", name = "X", adult = true, poster = image(100), adultPoster = image(900))

        expectThat(dto.toPartialMedia("letterboxd")!!.posterImage).isEqualTo("https://img/900.jpg")
    }

    @Test
    fun `maps a detail response to a movie`() {
        val dto =
            FilmDto(
                id = "b8wK",
                name = "Heat",
                releaseDate = "1995-12-15",
                runTime = 170,
                description = "A crime saga.",
                tagline = "A Los Angeles crime saga.",
                primaryLanguage = LanguageDto(code = "en", name = "English"),
                directors = listOf(ContributorSummaryDto(id = "c1", name = "Michael Mann")),
                trailer = TrailerDto(id = "yt", url = "https://youtube.com/watch?v=yt"),
                links = listOf(LinkDto(type = "tmdb", id = "949")),
            )

        val movie = dto.toMovie("letterboxd")

        expectThat(movie.type).isEqualTo(MediaType.MOVIE)
        expectThat(movie.title).isEqualTo("Heat")
        expectThat(movie.runtime).isEqualTo(170)
        expectThat(movie.tagLine).isEqualTo("A Los Angeles crime saga.")
        expectThat(movie.language).isEqualTo("en")
        expectThat(movie.releaseDate).isEqualTo(epochOf(1995, 12, 15))
        expectThat(movie.externalIds[MediaIdSource.TMDB]).isEqualTo("949")
        expectThat(movie.casts.map { it.name }).contains("Michael Mann")
        expectThat(movie.customProperties["trailer"]).isEqualTo("https://youtube.com/watch?v=yt")
    }

    @Test
    fun `directors are surfaced as credited cast`() {
        val dto = FilmDto(id = "x", name = "X", directors = listOf(ContributorSummaryDto(name = "Agnes Varda")))

        val cast = dto.toMovie("letterboxd").casts.single()

        expectThat(cast.name).isEqualTo("Agnes Varda")
        expectThat(cast.character).isEqualTo("Director")
    }

    @Test
    fun `maps a member summary and falls back to the username`() {
        val dto = MemberSummaryDto(id = "m1", username = "rhenwinch", displayName = "", avatar = image(200))

        val member = dto.toMember()

        expectThat(member.id).isEqualTo("m1")
        expectThat(member.displayName).isEqualTo("rhenwinch")
        expectThat(member.avatarUrl).isEqualTo("https://img/200.jpg")
    }
}
