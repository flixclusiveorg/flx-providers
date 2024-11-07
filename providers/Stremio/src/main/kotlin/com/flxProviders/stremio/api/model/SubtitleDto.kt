package com.flxProviders.stremio.api.model

import com.flixclusive.model.provider.link.Subtitle

internal data class SubtitleDto(
    val id: String,
    val url: String,
    val lang: String,
    val title: String? = null
) {
    private fun formatSubtitleName(addonName: String): String {
        return """
            Addon: $addonName
            Subtitle: $lang
            ID: $id
        """.trimIndent()
            .trim()
    }

    fun toSubtitle(addonName: String): Subtitle
        = Subtitle(
            language = formatSubtitleName(addonName),
            url = url,
        )
}

internal data class SubtitleResponse(
    val subtitles: List<SubtitleDto>,
    override val err: String?,
) : CommonErrorResponse()