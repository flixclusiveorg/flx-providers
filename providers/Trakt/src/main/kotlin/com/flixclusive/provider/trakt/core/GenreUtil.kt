package com.flixclusive.provider.trakt.core

import com.flixclusive.model.provider.Catalog
import com.flixclusive.provider.trakt.core.config.TraktApiConfig
import java.net.URLEncoder
import java.time.ZoneOffset
import java.time.ZonedDateTime

internal object GenreUtil {
    fun String.toCatalog(providerId: String): Catalog {
        val yearTodayPlus5 = ZonedDateTime.now(ZoneOffset.UTC).year + 5

        return Catalog(
            name = "Trakt's ${replaceFirstChar { it.titlecase() }}",
            url = buildString {
                append("${TraktApiConfig.API_BASE_URL}/media/trending")
                append("?genres=${URLEncoder.encode(this@toCatalog, "UTF-8")}")
                append("&extended=full%2Cimages")
                append("&years=1932-$yearTodayPlus5")
                append("&streaming_country=us")
            },
            providerId = providerId,
            canPaginate = true,
            description = "Trakt's ${replaceFirstChar { it.titlecase() }} genre catalog",
        )
    }
}