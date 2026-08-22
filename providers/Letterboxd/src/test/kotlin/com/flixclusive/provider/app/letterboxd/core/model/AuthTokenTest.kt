package com.flixclusive.provider.app.letterboxd.core.model

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

private fun tokenExpiringIn(millis: Long) =
    AuthToken(
        accessToken = "access",
        refreshToken = "refresh",
        expiresAt = System.currentTimeMillis() + millis,
    )

class AuthTokenTest {
    @Test
    fun `a token well inside its lifetime is neither expired nor due for refresh`() {
        val token = tokenExpiringIn(60 * 60 * 1000L)

        expectThat(token.isExpired).isFalse()
        expectThat(token.needsRefresh).isFalse()
    }

    @Test
    fun `a token inside the refresh window is not yet expired but needs refreshing`() {
        val token = tokenExpiringIn(60 * 1000L)

        expectThat(token.isExpired).isFalse()
        expectThat(token.needsRefresh).isTrue()
    }

    @Test
    fun `a lapsed token is both expired and due for refresh`() {
        val token = tokenExpiringIn(-1000L)

        expectThat(token.isExpired).isTrue()
        expectThat(token.needsRefresh).isTrue()
    }

    @Test
    fun `builds a bearer header value`() {
        expectThat(tokenExpiringIn(1000L).bearer).isEqualTo("Bearer access")
    }
}
