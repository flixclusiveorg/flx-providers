package com.flixclusive.provider.app.letterboxd.core.model.dto

import com.flixclusive.core.util.network.json.fromJson
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

/** Bodies below are verbatim from live `/auth/token` responses. */
class TokenResponseTest {
    @Test
    fun `parses a client credentials response, which carries no refresh token`() {
        val body = """{"access_token":"abc","token_type":"Bearer","expires_in":3600}"""

        val dto = fromJson<TokenResponseDto>(body)

        expectThat(dto.access_token).isEqualTo("abc")
        expectThat(dto.token_type).isEqualTo("Bearer")
        expectThat(dto.expires_in).isEqualTo(3600L)
        expectThat(dto.refresh_token).isEqualTo("")
    }

    @Test
    fun `parses a password grant response that includes a refresh token`() {
        val body =
            """{"access_token":"abc","refresh_token":"def","token_type":"Bearer","expires_in":3600}"""

        val dto = fromJson<TokenResponseDto>(body)

        expectThat(dto.refresh_token).isEqualTo("def")
    }

    @Test
    fun `surfaces the human-readable description from a rejected sign-in`() {
        val body =
            """{"error":"invalid_grant","error_description":"Your credentials don't match. """ +
                """It's probably attributable to human error."}"""

        val dto = fromJson<TokenErrorDto>(body)

        expectThat(dto.error).isEqualTo("invalid_grant")
        expectThat(dto.error_description.startsWith("Your credentials don't match")).isTrue()
    }

    @Test
    fun `an error body with no description still parses`() {
        val dto = fromJson<TokenErrorDto>("""{"error":"unauthorized_client"}""")

        expectThat(dto.error).isEqualTo("unauthorized_client")
        expectThat(dto.error_description).isEqualTo("")
    }
}
