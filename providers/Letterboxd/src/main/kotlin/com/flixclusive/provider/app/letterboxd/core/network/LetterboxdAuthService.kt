package com.flixclusive.provider.app.letterboxd.core.network

import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.HttpMethod
import com.flixclusive.core.util.network.okhttp.formRequest
import com.flixclusive.provider.app.letterboxd.core.config.LetterboxdConfig
import com.flixclusive.provider.app.letterboxd.core.model.AuthToken
import com.flixclusive.provider.app.letterboxd.core.model.dto.TokenErrorDto
import com.flixclusive.provider.app.letterboxd.core.model.dto.TokenResponseDto
import okhttp3.Headers
import okhttp3.OkHttpClient

/** Carries a message safe to show the user — never the raw response body. */
internal class LetterboxdAuthException(
    message: String,
) : IllegalStateException(message)

/**
 * Letterboxd retired HMAC request signing — "this requirement has been removed" — so
 * every call is plain OAuth2 with a bearer header. Do not reintroduce signing.
 *
 * Sign-in uses the Password grant, which keeps the whole flow inside the settings screen
 * with no browser hand-off and no redirect URI. Letterboxd documents that grant as
 * first-party only, so it may stop working without notice; the other grants here are
 * unaffected if it does.
 */
internal class LetterboxdAuthService(
    private val client: OkHttpClient,
) {
    suspend fun clientCredentials(): AuthToken =
        token(
            mapOf(
                "grant_type" to "client_credentials",
                "client_id" to LetterboxdConfig.clientId,
                "client_secret" to LetterboxdConfig.clientSecret,
            ),
        )

    suspend fun signIn(
        username: String,
        password: String,
    ): AuthToken =
        token(
            mapOf(
                "grant_type" to "password",
                "username" to username,
                "password" to password,
                "client_id" to LetterboxdConfig.clientId,
                "client_secret" to LetterboxdConfig.clientSecret,
            ),
        )

    suspend fun refresh(refreshToken: String): AuthToken =
        token(
            mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
                "client_id" to LetterboxdConfig.clientId,
                "client_secret" to LetterboxdConfig.clientSecret,
            ),
        )

    private suspend fun token(form: Map<String, String>): AuthToken =
        FlxDispatchers.withIOContext {
            val response =
                client
                    .formRequest(
                        url = LetterboxdConfig.TOKEN_URL,
                        method = HttpMethod.POST,
                        body = form,
                        headers = Headers.headersOf("Accept", "application/json"),
                    ).execute()

            response.use {
                val body = it.body.string()

                if (!it.isSuccessful) {
                    // Only the two documented error fields are surfaced; the raw body is
                    // never echoed, since the request that produced it carried the secret.
                    val described =
                        safeCall { fromJson<TokenErrorDto>(body) }
                            ?.error_description
                            ?.takeIf { message -> message.isNotBlank() }

                    throw LetterboxdAuthException(
                        described ?: "Letterboxd rejected the request (HTTP ${it.code}).",
                    )
                }

                val dto = fromJson<TokenResponseDto>(body)
                if (dto.access_token.isBlank()) {
                    throw LetterboxdAuthException("Letterboxd returned no access token.")
                }

                AuthToken(
                    accessToken = dto.access_token,
                    refreshToken = dto.refresh_token,
                    expiresAt = System.currentTimeMillis() + dto.expires_in * 1000L,
                    tokenType = dto.token_type,
                )
            }
        }
}
