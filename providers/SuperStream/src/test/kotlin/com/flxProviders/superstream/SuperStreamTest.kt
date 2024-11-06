package com.flxProviders.superstream

import android.content.Context
import android.util.Base64
import com.flixclusive.core.util.log.LogRule
import com.flixclusive.core.util.log.debugLog
import com.flixclusive.model.film.FilmDetails
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import com.flxProviders.superstream.util.CipherUtil.decrypt
import com.flxProviders.superstream.util.Constants
import com.flxProviders.superstream.util.CustomCertificateClient
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileInputStream


class SuperStreamTest {
    @get:Rule
    val logRule = LogRule()
    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = TestScope(UnconfinedTestDispatcher())

    private lateinit var api: ProviderApi

    private val movie = Movie(
        tmdbId = 299534,
        imdbId = "tt4154796",
        title = "Avengers: Endgame",
        posterImage = null,
        homePage = null,
        id = null,
        providerName = "TMDB"
    )

    private val tvShow = TvShow(
        tmdbId = 94605,
        imdbId = "tt11126994",
        title = "Arcane",
        posterImage = null,
        homePage = null,
        id = null,
        providerName = "TMDB"
    )

    @Before
    fun setUp() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            val byteArray = arg<ByteArray>(0)
            java.util.Base64.getEncoder().encodeToString(byteArray)
        }
        every { Base64.encode(any(), any()) } answers {
            val byteArray = arg<String>(0).toByteArray()
            java.util.Base64.getEncoder().encode(byteArray)
        }
        every { Base64.decode(any<String>(), any()) } answers {
            val byteArray = arg<String>(0).toByteArray()
            java.util.Base64.getDecoder().decode(byteArray)
        }
        every { Base64.decode(any<ByteArray>(), any()) } answers {
            java.util.Base64.getDecoder().decode(arg<ByteArray>(0))
        }

        val certFile = File("src/test/certs/cert.crt")
        val keyFile = File("src/test/certs/key.key")
        val caFile = File("src/test/certs/ca.crt")
        val ca4File = File("src/test/certs/ca4.cer")

        val certStream = FileInputStream(certFile)
        val keyStream = FileInputStream(keyFile)
        val caStream = FileInputStream(caFile)
        val ca4Stream = FileInputStream(ca4File)

        val customCertificate = CustomCertificateClient()
        val client = customCertificate.createOkHttpClient(
            clientCertStream = certStream,
            caCertStreams = listOf(caStream, ca4Stream),
            privateKeyInputStream = keyStream,
            privateKeyPassword = null
        )

        api = SuperStreamTestApi(
            client,
            object: Provider() {
                override fun getApi(context: Context, client: OkHttpClient): ProviderApi {
                    throw NotImplementedError()
                }
            }
        )
    }

    @Test
    fun `get movie links`() = scope.runTest {
        val watchId = getWatchId(movie)
        assertNotNull(watchId) {
            "WatchId cannot be null"
        }
        debugLog("WatchId: $watchId")

        val links = mutableListOf<MediaLink>()
        api.getLinks(
            watchId = watchId!!,
            film = movie,
            onLinkFound = links::add
        )

        debugLog("Link: $links")
        assert(links.size > 0) {
            "No links loaded"
        }
    }

    @Test
    fun `get tv show links`() = scope.runTest {
        val watchId = getWatchId(tvShow)
        assertNotNull(watchId) {
            "WatchId cannot be null"
        }
        debugLog("WatchId: $watchId")

        val links = mutableListOf<MediaLink>()
        api.getLinks(
            watchId = watchId!!,
            film = tvShow,
            episode = Episode(season = 1, number = 1),
            onLinkFound = links::add
        )

        debugLog("Link: $links")
        assert(links.size > 0) {
            "No links loaded"
        }
    }

    @Test
    fun `get movie details`() = scope.runTest {
        val watchId = getWatchId(movie)
        assertNotNull(watchId) {
            "Watch ID cannot be null"
        }
        val metadata = api.getFilmDetails(film = movie.copy(id = watchId))

        assert(metadata is Movie)
        println(metadata)
    }

    @Test
    fun `get tv show details`() = scope.runTest {
        val watchId = getWatchId(tvShow)
        assertNotNull(watchId) {
            "Watch ID cannot be null"
        }
        val metadata = api.getFilmDetails(film = tvShow.copy(id = watchId))

        assert(metadata is TvShow)
        println(metadata)
    }

    @Test
    fun `search for Avengers Endgame`() = scope.runTest {
        val response = api.search(
            title = movie.title,
            imdbId = movie.imdbId,
        )

        assert(response.results.isNotEmpty()) {
            "No search results found for ${movie.title}"
        }

        debugLog("Search results: ${response.results}")
    }

    private suspend fun getWatchId(film: FilmDetails): String? {
        return api.search(
            title = film.title,
            imdbId = film.imdbId,
        ).results.firstOrNull()?.id
    }

    @Test
    fun decryption() {
        var data = "eyJhcHBfa2V5IjoiNjQwNWMyZTYwNDVmNjU5YjdmZGNkOTU3ZmU1MmZlOWEiLCJ2ZXJpZnkiOiI4YWY0ZDBmNjBiN2RlYmRkMjdmMjUyMjIyODk4NjQ1MCIsImVuY3J5cHRfZGF0YSI6IkZER2kwcGV3R2MyUklWdTJKM2ltWVR0Wm5aOWFtTFFqQVBCV3lPY3d4SlpxSXgwUVN3WWN2Nzc3Qy9WSTVHVmVQUWx4M0phNVBick9lZG9tSFRoSWNTYWY1Y3p6U2dqTHpnVWlVUDBKL3E1dFBZTzhqTnZZby9HTit4K0M0V1hJUmw2UmNwa2NpY01BWEdTMnRKUi81VVkzbTFwZm90R2tRcUxKV1JxTkw3cTIwZ1ZOMXlzRWtQSll0ZW9PRkNTVEVYcWRiU05XM2M5aUdGbDEra09LYVJBRU9HK2dwVmYwOFpTeUYxdEtZTUQwMFd4NW1kRzFIS25tcTllT1AzMk9IeC9BdHBxQXRwdTVxd25VdE5aa3ZYT0ZaaitVY1dleW0rVTh5eTdydFM2Q0pzdU81Y01TcDY3cFI4RkNWNlI5cEZzTDZISVhQbTU1VytOTDBKQ2c2QmhqL0RpaUZ2bEsifQ"

        if (data.startsWith("ey")) {
            val decodedJson = String(java.util.Base64.getDecoder().decode(data))

            val regex = """"encrypt_data":"(.*?)"""".toRegex()
            val matchResult = regex.find(decodedJson)

            data = matchResult?.groups?.get(1)?.value!!
        }

        val result = decrypt(data, Constants.API_KEY, Constants.API_IV)
        print(result)

        assertNotNull(result)
    }
}