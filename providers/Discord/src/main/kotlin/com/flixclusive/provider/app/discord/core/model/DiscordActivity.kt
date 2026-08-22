package com.flixclusive.provider.app.discord.core.model

internal data class DiscordButton(
    val label: String,
    val url: String,
)

internal data class DiscordActivity(
    val name: String,
    val type: Int = TYPE_WATCHING,
    val details: String? = null,
    val state: String? = null,
    val startTimestamp: Long? = null,
    val endTimestamp: Long? = null,
    val largeImage: String? = null,
    val largeText: String? = null,
    val buttons: List<DiscordButton> = emptyList(),
) {
    companion object {
        const val TYPE_PLAYING = 0
        const val TYPE_LISTENING = 2
        const val TYPE_WATCHING = 3
        const val MAX_BUTTONS = 2
    }
}
