package com.flxProviders.superstream.api.dto

import com.flixclusive.model.film.util.FilmType

enum class BoxType(val value: Int) {
    Series(value = 2),
    Movies(value = 1);

    companion object {
        fun fromFilmType(filmType: FilmType): BoxType {
            return when (filmType) {
                FilmType.TV_SHOW -> Series
                FilmType.MOVIE -> Movies
            }
        }
    }
}