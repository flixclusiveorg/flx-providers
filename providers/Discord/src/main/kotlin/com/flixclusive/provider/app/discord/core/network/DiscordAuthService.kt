package com.flixclusive.provider.app.discord.core.network

import android.util.Base64
import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.HttpMethod
import com.flixclusive.core.util.network.okhttp.formRequest
import com.flixclusive.provider.app.discord.core.config.DiscordConfig
import com.flixclusive.provider.app.discord.core.model.DiscordAuthToken
import com.flixclusive.provider.app.discord.core.network.dto.TokenResponse
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

internal data class PkcePair(
    val verifier: String,
    val challenge: String,
)

internal class DiscordAuthException(message: String) : Exception(message)

internal class DiscordAuthService(
    private val client: OkHttpClient,
) {
    fun generatePkcePair(): PkcePair {
        val verifier = randomUrlSafe(VERIFIER_BYTES)
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return PkcePair(verifier = verifier, challenge = encode(digest))
    }

    fun generateState(): String = randomUrlSafe(STATE_BYTES)

    suspend fun exchangeCode(
        code: String,
        verifier: String,
        redirectUri: String,
    ): DiscordAuthToken =
        requestToken(
            mapOf(
                "client_id" to DiscordConfig.CLIENT_ID,
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to redirectUri,
                "code_verifier" to verifier,
            ),
        )

    suspend fun refresh(refreshToken: String): DiscordAuthToken =
        requestToken(
            mapOf(
                "client_id" to DiscordConfig.CLIENT_ID,
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
            ),
        )

    private suspend fun requestToken(form: Map<String, String>): DiscordAuthToken =
        FlxDispatchers.withIOContext {
            client
                .formRequest(
                    url = DiscordConfig.OAUTH_TOKEN_URL,
                    method = HttpMethod.POST,
                    body = form,
                ).execute()
                .use { response ->
                    val body = response.body.string()

                    if (!response.isSuccessful) {
                        throw DiscordAuthException(
                            "Discord token request failed (HTTP ${response.code}, ${body.errorCode()}).",
                        )
                    }

                    fromJson<TokenResponse>(body).toAuthToken()
                }
        }

    private fun randomUrlSafe(byteCount: Int): String {
        val bytes = ByteArray(byteCount).also(SecureRandom()::nextBytes)
        return encode(bytes)
    }

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    private companion object {
        const val VERIFIER_BYTES = 64
        const val STATE_BYTES = 16
    }
}

private fun String.errorCode(): String =
    runCatching { JSONObject(this).optString("error").takeIf(String::isNotBlank) }
        .getOrNull() ?: "no error code"
