package com.flixclusive.provider.app.letterboxd.core.network

import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.core.util.network.okhttp.HttpMethod
import com.flixclusive.core.util.network.okhttp.genericBodyRequest
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.provider.app.letterboxd.core.config.LetterboxdConfig
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient

internal class LetterboxdAuthRequiredException : IllegalStateException("Sign in to Letterboxd to use this feature.")

internal class LetterboxdHttpException(
    val code: Int,
    message: String,
) : IllegalStateException(message)

/**
 * Auth is attached per request rather than through an interceptor: the token lookup is
 * suspending, and an interceptor could only reach it via `runBlocking` on OkHttp's own
 * dispatcher thread. Attaching it here also means no header is sent at all when there
 * is no token, instead of a literal `Bearer null`.
 */
internal class LetterboxdApi(
    private val readClient: OkHttpClient,
    private val writeClient: OkHttpClient,
    private val tokens: LetterboxdTokenProvider,
) {
    suspend fun get(
        path: String,
        params: List<Pair<String, String>> = emptyList(),
        requireMember: Boolean = false,
    ): String =
        FlxDispatchers.withIOContext {
            val url = buildUrl(path, params)
            val call = readClient.request(url = url, headers = headers(requireMember))
            call.execute().use { response -> readBody(response, path) }
        }

    suspend fun send(
        method: HttpMethod,
        path: String,
        body: String? = null,
        params: List<Pair<String, String>> = emptyList(),
    ): String =
        FlxDispatchers.withIOContext {
            val url = buildUrl(path, params)
            val headers = headers(requireMember = true)

            val call =
                if (body == null) {
                    writeClient.request(url = url, method = method, headers = headers)
                } else {
                    writeClient.genericBodyRequest(
                        url = url,
                        method = method,
                        body = body,
                        mediaType = JSON_MEDIA_TYPE,
                        headers = headers,
                    )
                }

            call.execute().use { response -> readBody(response, path) }
        }

    private suspend fun headers(requireMember: Boolean): Headers {
        val bearer =
            if (requireMember) {
                tokens.memberBearer() ?: throw LetterboxdAuthRequiredException()
            } else {
                tokens.bearer()
            }

        val builder = Headers.Builder().add("Accept", "application/json")
        if (bearer != null) builder.add("Authorization", bearer)

        return builder.build()
    }

    private fun readBody(
        response: okhttp3.Response,
        path: String,
    ): String {
        val body = response.body.string()

        if (!response.isSuccessful) {
            throw LetterboxdHttpException(
                code = response.code,
                message = "Letterboxd $path failed (HTTP ${response.code})",
            )
        }

        return body
    }

    private fun buildUrl(
        path: String,
        params: List<Pair<String, String>>,
    ): String =
        buildString {
            append(LetterboxdConfig.BASE_URL)
            append('/')
            append(path.removePrefix("/"))

            val usable = params.filter { it.second.isNotBlank() }
            if (usable.isEmpty()) return@buildString

            append('?')
            usable.joinTo(this, "&") { (key, value) ->
                "${LetterboxdConfig.encode(key)}=${LetterboxdConfig.encode(value)}"
            }
        }
}

private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
