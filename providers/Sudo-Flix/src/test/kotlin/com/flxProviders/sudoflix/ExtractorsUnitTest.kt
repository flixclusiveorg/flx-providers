package com.flxProviders.sudoflix

import com.flixclusive.core.util.network.ignoreAllSSLErrors
import com.flxProviders.sudoflix.api.primewire.extractor.DoodStream
import com.flxProviders.sudoflix.api.primewire.extractor.DropLoad
import com.flxProviders.sudoflix.api.primewire.extractor.FileLions
import com.flxProviders.sudoflix.api.primewire.extractor.MixDrop
import com.flxProviders.sudoflix.api.primewire.extractor.StreamVid
import com.flxProviders.sudoflix.api.primewire.extractor.StreamWish
import com.flxProviders.sudoflix.api.primewire.extractor.UpStream
import com.flxProviders.sudoflix.api.primewire.extractor.VTube
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test

class ExtractorsUnitTest {
    private lateinit var client: OkHttpClient

    @Before
    fun setup() {
        client = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .ignoreAllSSLErrors()
            .build()
    }

    @Test
    fun mixDropExtractorTest() = runTest {
        val url = "https://www.primewire.tf/links/go/eVG7I"
        val extractor = MixDrop(client)

        val links = extractor.extract(url = url)
        assert(links.isNotEmpty())
    }

    @Test
    fun upStreamExtractorTest() = runTest {
        val url = "https://www.primewire.tf/links/go/nSf_-"
        val extractor = UpStream(client)

        val links = extractor.extract(url = url)
        assert(links.isNotEmpty())
    }

    @Test
    fun fileLionsExtractorTest() = runTest {
        val url = "https://www.primewire.tf/links/go/eJIxn"
        val extractor = FileLions(client)

        val links = extractor.extract(url = url)
        assert(links.isNotEmpty())
    }

    @Test
    fun doodStreamExtractorTest() = runTest {
        val url = "https://www.primewire.tf/links/go/ie9Xp"
        val extractor = DoodStream(client)

        val links = extractor.extract(url = url)
        assert(links.isNotEmpty())
    }

    @Test
    fun dropLoadExtractorTest() = runTest {
        val url = "https://www.primewire.tf/links/go/O0DT7"
        val extractor = DropLoad(client)

        val links = extractor.extract(url = url)
        assert(links.isNotEmpty())
    }

    @Test
    fun streamVidExtractorTest() = runTest {
        val url = "https://www.primewire.tf/links/go/zPf7r"
        val extractor = StreamVid(client)

        val links = extractor.extract(url = url)
        assert(links.isNotEmpty())
    }

    @Test
    fun vTubeExtractorTest() = runTest {
        val url = "https://www.primewire.tf/links/go/H_KTs"
        val extractor = VTube(client)

        val links = extractor.extract(url = url)
        assert(links.isNotEmpty())
    }

    @Test
    fun streamWishExtractorTest() = runTest {
        val url = "https://www.primewire.tf/links/go/RTaQj"
        val extractor = StreamWish(client)

        val links = extractor.extract(url = url)
        assert(links.isNotEmpty())
    }
}