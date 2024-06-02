package com.flxProviders.superstream.api.util

import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.CryptographyUtil.base64Encode
import com.flixclusive.core.util.network.HttpMethod
import com.flixclusive.core.util.network.formRequest
import com.flixclusive.core.util.network.fromJson
import com.flxProviders.superstream.BuildConfig
import com.flxProviders.superstream.api.util.Constants.appKey
import com.flxProviders.superstream.api.util.Constants.appVersionCode
import com.flxProviders.superstream.api.util.Constants.headers
import com.flxProviders.superstream.api.util.Constants.iv
import com.flxProviders.superstream.api.util.Constants.key
import com.flxProviders.superstream.api.util.SuperStreamUtil.randomToken
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient

internal inline fun <reified T : Any> OkHttpClient.superStreamCall(
    query: String,
    useAlternativeApi: Boolean = false,
): T? {
    val encryptedQuery = CipherUtil.encrypt(query, key, iv)!!
    val appKeyHash = MD5Util.md5(appKey)!!
    val verify = CipherUtil.getVerify(encryptedQuery, appKey, key)
    val newBody =
        """{"app_key":"$appKeyHash","verify":"$verify","encrypt_data":"$encryptedQuery"}"""

    val data = mapOf(
        "data" to base64Encode(newBody.toByteArray()),
        "appid" to "27",
        "platform" to "android",
        "version" to appVersionCode,
        "medium" to "Website&token${randomToken()}"
    )

    val errorMessage = "Failed to fetch SuperStream API"
    val url = if (useAlternativeApi) BuildConfig.SUPERSTREAM_SECOND_API else BuildConfig.SUPERSTREAM_FIRST_API
    val response = this.formRequest(
        url = url,
        method = HttpMethod.POST,
        body = data,
        headers = headers.toHeaders()
    ).execute()

    val responseBody = response.body?.string()
        ?: throw Exception(errorMessage + " [${response.code} - ${response.message}]")

    if(
        responseBody.contains(
            other = """"msg":"success""",
            ignoreCase = true
        ).not()
    ) {
        errorLog("SuperStream call response body: $responseBody")
        throw Exception(errorMessage + " [${response.code} - ${response.message}]")
    }

    if (response.isSuccessful && responseBody.isNotBlank())
        return fromJson(responseBody)
    else throw Exception("$errorMessage: [${responseBody}]")
}