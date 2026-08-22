package com.flixclusive.provider.app.discord.core.presence

import kotlin.random.Random

internal sealed interface ReconnectAction {
    data class Resume(val sessionId: String, val seq: Int) : ReconnectAction

    data object ReIdentify : ReconnectAction

    data object RefreshAndReIdentify : ReconnectAction

    data object SurfaceFatal : ReconnectAction
}

internal object DiscordReconnectStrategy {
    const val MAX_ATTEMPTS = 7

    private const val BASE_DELAY_MS = 1_000L
    private const val MAX_DELAY_MS = 64_000L
    private const val JITTER_RATIO = 0.25

    fun decide(
        closeCode: Int,
        hadSession: Boolean,
        seq: Int,
        sessionId: String?,
    ): ReconnectAction =
        when (closeCode) {
            4000 ->
                if (hadSession && sessionId != null && seq > 0) {
                    ReconnectAction.Resume(sessionId, seq)
                } else {
                    ReconnectAction.ReIdentify
                }

            4004 -> ReconnectAction.RefreshAndReIdentify
            4012, 4013, 4014 -> ReconnectAction.SurfaceFatal
            else -> ReconnectAction.ReIdentify
        }

    fun backoffMillis(
        attempt: Int,
        random: Random = Random.Default,
    ): Long {
        val exponent = (attempt - 1).coerceIn(0, 6)
        val base = (BASE_DELAY_MS shl exponent).coerceAtMost(MAX_DELAY_MS)
        val jitter = (base * JITTER_RATIO).toLong()
        val offset = if (jitter == 0L) 0L else random.nextLong(-jitter, jitter + 1)
        return (base + offset).coerceIn(BASE_DELAY_MS, MAX_DELAY_MS)
    }
}
