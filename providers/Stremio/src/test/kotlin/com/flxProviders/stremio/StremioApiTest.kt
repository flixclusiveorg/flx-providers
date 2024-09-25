package com.flxProviders.stremio

import com.flixclusive.core.util.log.LogRule
import com.flixclusive.core.util.log.debugLog
import com.flixclusive.core.util.network.json.fromJson
import com.flxProviders.stremio.api.StremioApi
import com.flxProviders.stremio.api.model.FetchCatalogResponse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class StremioApiTest {
    private lateinit var stremioApi: StremioApi

    @get:Rule
    val rule = LogRule()

    @Before
    fun setUp() {
    }

    @Test
    fun `test GSON for Metas`() {
        val metas = fromJson<FetchCatalogResponse>(
            """
                {"metas":[{"id":"kitsu:47635","type":"series","animeType":"TV","name":"Kimetsu no Yaiba: Hashira Geiko-hen","aliases":["Demon Slayer: Kimetsu no Yaiba - Hashira Training Arc","Kimetsu no Yaiba: Hashira Geiko-hen","KnY 4","Demon Slayer Season 4"],"description":"The members of the Demon Slayer Corps and their highest-ranking swordsmen, the Hashira.\nIn preparation for the forthcoming final battle against Muzan Kibutsuji, the Hashira Training commences. While each carries faith and determination within their hearts, Tanjiro and the Hashira enter a new story.\n\n(Source: Crunchyroll)\n\nNotes:\n• The first episode has a runtime of ~1 hour and received an early premiere in cinemas as part of a special screening alongside the final episode of Kimetsu no Yaiba: Katanakaji no Sato-hen.","releaseInfo":"2024-","runtime":"23 min","imdbRating":"8.4","genres":[],"logo":"https://assets.fanart.tv/fanart/tv/348545/hdtvlogo/demon-slayer-kimetsu-no-yaiba-64bd402213d73.png","poster":"https://media.kitsu.io/anime/47635/poster_image/medium-d0ef3a22404a5d3eb9985d0a6743f227.jpeg","background":"https://assets.fanart.tv/fanart/tv/348545/showbackground/demon-slayer-kimetsu-no-yaiba-5fee3da0d0cff.jpg","trailers":[{"source":"PraFso1sVIc","type":"Trailer"}],"links":[{"name":"8.4","category":"imdb","url":"https://kitsu.io/anime/Kimetsu-no-Yaiba-Hashira-Geikohen"}]}],"cacheMaxAge":43200}
            """.trimIndent()
        )

        debugLog(metas.items)
        assert(metas.items?.isNotEmpty() == true)
    }
}