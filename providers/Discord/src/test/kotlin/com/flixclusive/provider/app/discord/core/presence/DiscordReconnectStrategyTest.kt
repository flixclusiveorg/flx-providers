package com.flixclusive.provider.app.discord.core.presence

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isLessThanOrEqualTo

class DiscordReconnectStrategyTest {
    @Test
    fun `resumes when a session and sequence are available`() {
        val action = DiscordReconnectStrategy.decide(4000, hadSession = true, seq = 12, sessionId = "abc")

        expectThat(action).isEqualTo(ReconnectAction.Resume("abc", 12))
    }

    @Test
    fun `re-identifies on 4000 when there is no session to resume`() {
        val action = DiscordReconnectStrategy.decide(4000, hadSession = false, seq = 0, sessionId = null)

        expectThat(action).isA<ReconnectAction.ReIdentify>()
    }

    @Test
    fun `refreshes the token when authentication fails`() {
        val action = DiscordReconnectStrategy.decide(4004, hadSession = true, seq = 5, sessionId = "abc")

        expectThat(action).isA<ReconnectAction.RefreshAndReIdentify>()
    }

    @Test
    fun `treats permanent misconfiguration codes as fatal`() {
        listOf(4012, 4013, 4014).forEach { code ->
            val action = DiscordReconnectStrategy.decide(code, hadSession = true, seq = 5, sessionId = "abc")

            expectThat(action).isA<ReconnectAction.SurfaceFatal>()
        }
    }

    @Test
    fun `re-identifies on unknown close codes`() {
        val action = DiscordReconnectStrategy.decide(1006, hadSession = true, seq = 5, sessionId = "abc")

        expectThat(action).isA<ReconnectAction.ReIdentify>()
    }

    @Test
    fun `backoff grows and never exceeds the cap`() {
        (1..12).forEach { attempt ->
            expectThat(DiscordReconnectStrategy.backoffMillis(attempt))
                .isGreaterThanOrEqualTo(1_000L)
                .isLessThanOrEqualTo(64_000L)
        }
    }
}
