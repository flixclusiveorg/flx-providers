package com.flxProviders.superstream

import com.flixclusive.provider.testing.BaseProviderTest
import com.flxProviders.superstream.api.SuperStreamApi
import com.flxProviders.superstream.api.util.CipherUtil.decrypt
import com.flxProviders.superstream.api.util.Constants
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * ## Template unit test for your providers.
 *
 * TIP: Uncomment the inheritance for [BaseProviderTest](https://github.com/rhenwinch/Flixclusive/blob/master/provider/base/src/main/kotlin/com/flixclusive/provider/base/testing/BaseProviderTest.kt)
 * if you're too lazy to create your own test cases
 *
 * This template provides a starting point for testing your provider classes.
 * Follow the Arrange-Act-Assert pattern for each test method.
 *
 * @see BaseProviderTest
 */
class SuperStreamApiUnitTest : BaseProviderTest()  {
    private val data = "eyJhcHBfa2V5IjoiNjQwNWMyZTYwNDVmNjU5YjdmZGNkOTU3ZmU1MmZlOWEiLCJ2ZXJpZnkiOiJmYzhjYzg0ODc5NDVjNjBmY2Y5YWJkMTNhZWU1ODNmNCIsImVuY3J5cHRfZGF0YSI6IkZER2kwcGV3R2MyUklWdTJKM2ltWVRVRU9abHZaM0xhdVo0c25YNXVaZUZWbi9ZYi9ROVA5WWFSMDViekhUWVpJYmIrQ0VpQzBwSVZuS0ZNR3AwYnVSTEFLS1pqbENOQm9oakNlaEpYSEF5NmRYSUxmN3RSY01jTXR3TElYWFlqd2xtdjJEQlQxaFRmNS9xU29QZlJKZlRXT1FJU3Q2eWhWQ3I4OG9zNEl6QldSZnV4ZmhuMDFScS9keVRRNCszSFlodUwvdTlxZXZmZkxyTnVOWmhnd1kvdDArNnZVSzNwR2dXU09ablZGRWtnY3ZvV1k3RFNTelNuTXBXMTJpKzFFQVpNTXFRNHNDVGYydURyVW9ZeklQQlZTOU9CbDY1emF0WnJVMHdPYkp3PSJ9"

    @Before
    override fun setUp() {
        super.setUp()

        sourceProviderApi = SuperStreamApi(OkHttpClient(), SuperStream())
    }

    @Test
    fun decryption() {
        val result = decrypt(data, Constants.key, Constants.iv)

        assertNotNull(result)
    }


//    @Test
//    fun `Cloudfare check`() {
//        val client = OkHttpClient()
//        val mediaType = "application/x-www-form-urlencoded".toMediaType()
//        val body = "data=$data&appid=27&platform=android&version=160&medium=Website%26token=f8ed3c62d0d57a5823ccbe14b802ed57".toRequestBody(mediaType)
//
//        val responseBody = client.request(
//            url = apiUrl,
//            method = HttpMethod.POST,
//            body = body,
//            headers = headers.toHeaders()
//        ).execute().body?.string()
//
//        assertNotNull(responseBody)
//        assert(responseBody!!.contains("cloudfare", true).not())
//        println(responseBody)
//    }
}
