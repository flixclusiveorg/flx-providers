package com.flixclusive.provider.app.discord.core.model

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

private inline fun <reified T> roundTrip(value: T): T = Json.decodeFromString<T>(Json.encodeToString(value))

class DiscordAuthTokenTest {
    @Test
    fun `survives a round trip through a reified serializer in another class`() {
        val token =
            DiscordAuthToken(
                accessToken = "access",
                refreshToken = "refresh",
                expiresAt = 1_700_000_000_000L,
                scope = "openid sdk.social_layer_presence",
            )

        expectThat(roundTrip(token)).isEqualTo(token)
    }

    @Test
    fun `is expired once the deadline has passed`() {
        val token = DiscordAuthToken(accessToken = "a", expiresAt = System.currentTimeMillis() - 1)

        expectThat(token.isExpired).isTrue()
    }

    @Test
    fun `is not expired well before the deadline`() {
        val token = DiscordAuthToken(accessToken = "a", expiresAt = System.currentTimeMillis() + 7_200_000L)

        expectThat(token.isExpired).isFalse()
        expectThat(token.needsRefresh).isFalse()
    }

    @Test
    fun `needs refresh inside the refresh window`() {
        val token = DiscordAuthToken(accessToken = "a", expiresAt = System.currentTimeMillis() + 60_000L)

        expectThat(token.isExpired).isFalse()
        expectThat(token.needsRefresh).isTrue()
    }

    @Test
    fun `prefixes the access token for the gateway`() {
        expectThat(DiscordAuthToken(accessToken = "abc").bearer).isEqualTo("Bearer abc")
    }
}
