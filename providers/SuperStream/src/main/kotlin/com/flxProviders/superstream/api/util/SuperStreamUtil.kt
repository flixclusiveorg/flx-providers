package com.flxProviders.superstream.api.util

import com.flixclusive.core.util.film.FilmType
import java.text.SimpleDateFormat
import java.util.Locale

internal object SuperStreamUtil {

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

}