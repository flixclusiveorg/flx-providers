package com.flixclusive.provider.app.discord.core.presence

import com.flixclusive.provider.app.discord.core.model.DiscordActivity

internal interface DiscordPresenceClient {
    suspend fun update(activity: DiscordActivity)

    suspend fun clear()

    suspend fun disconnect()
}
