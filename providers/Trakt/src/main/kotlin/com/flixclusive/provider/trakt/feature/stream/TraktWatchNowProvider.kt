package com.flixclusive.provider.trakt.feature.stream

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import androidx.compose.ui.util.fastFirstOrNull
import com.flixclusive.core.util.coroutines.FlxDispatchers
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.provider.capability.MediaLinkProviderApi
import com.flixclusive.provider.capability.MediaLinkType
import com.flixclusive.provider.trakt.TraktPlugin
import com.flixclusive.provider.trakt.core.config.TraktApiConfig
import com.flixclusive.provider.trakt.core.model.TraktWatchNowSource
import com.flixclusive.provider.trakt.core.network.TraktApiService
import com.flixclusive.provider.trakt.core.network.dto.response.TraktMediaWatchNowSources
import com.flixclusive.provider.trakt.core.network.util.OkHttpClientUtil
import com.flixclusive.provider.trakt.feature.stream.ResourceUtil.getRawFileInputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TraktWatchNowProvider internal constructor(
    private val context: Context,
    private val plugin: TraktPlugin,
) : MediaLinkProviderApi {
    companion object {
        private const val DEFAULT_WATCHNOW_SOURCE = "us" // USA Country
    }

    val cache30DayClient by lazy {
        OkHttpClientUtil.createCachedClient(
            context = context,
            settings = plugin.settings,
            cacheMaxAge = 60 * 60 * 24 * 30 // 30 days in seconds
        )
    }

    private val cachedApiServiceForLinks by lazy {
        TraktApiService.create(
            OkHttpClientUtil.createCachedClient(
                context = context,
                settings = plugin.settings,
                cacheMaxAge = 60 * 60 * 24 * 10 // 10 days in seconds
            )
        )
    }

    // This is a separate instance of the API service with a different cache configuration,
    // specifically for fetching watch now sources because it is a big ass list!
    private val cachedApiServiceForWatchNowSources by lazy {
        TraktApiService.create(cache30DayClient)
    }

    override val supportedLinkTypes = setOf(MediaLinkType.STREAMS)

    override fun getLinks(
        media: MediaMetadata,
        episode: Episode?
    ): Flow<MediaLink> = flow {
        // current API doesn't allow us to use /watchnow/sources endpoint, hence the comment
        // val countryCode = getCountry(context)
        // val sourceInfos = cachedApiServiceForWatchNowSources.getWatchNowSources(countryCode)

        val countryCode = DEFAULT_WATCHNOW_SOURCE
        val sourceInfos = FlxDispatchers.withIOContext { getTemporarySourceInfos() }

        var rawStreams = getRawStreams(
            media = media,
            episode = episode,
            countryCode = countryCode
        )

        if (
            (rawStreams == null || rawStreams.isEmpty)
                // && countryCode != DEFAULT_WATCHNOW_SOURCE
        ) {
            rawStreams = getRawStreams(
                media = media,
                episode = episode,
                countryCode = DEFAULT_WATCHNOW_SOURCE
            )
        }

        rawStreams?.all?.mapAsync { rawStream ->
            val sourceInfo = sourceInfos?.fastFirstOrNull { it.id == rawStream.id }
            if (sourceInfo == null) {
                errorLog("Failed to find source info for raw stream ${rawStream.id} in country $countryCode")
                return@mapAsync null
            }

            val link = rawStream.linkAndroid ?: rawStream.linkDirect ?: rawStream.link
            val stream = Stream(
                url = link,
                name = sourceInfo.name,
                description = rawStream.description,
                flags = setOf(
                    Flag.ThirdPartyGateway(
                        name = sourceInfo.name,
                        logo = sourceInfo.logo,
                    )
                )
            )

            emit(stream)
        }
    }

    private suspend fun getTemporarySourceInfos(): List<TraktWatchNowSource>? {
        val resources = plugin.resources
        requireNotNull(resources) {
            "This provider requires resources but the app hasn't provide it with any."
        }

        val sourceInfoAsString = FlxDispatchers.withIOContext {
            getRawFileInputStream(
                resources = plugin.resources!!,
                fileName = "us_watchnow"
            ).bufferedReader()
                .use { it.readText() }
        }

        return fromJson<List<Map<String, List<TraktWatchNowSource>>>>(json = sourceInfoAsString)
            .firstOrNull()
            ?.values
            ?.firstOrNull()
    }

    private suspend fun getRawStreams(
        media: MediaMetadata,
        episode: Episode?,
        countryCode: String
    ): TraktMediaWatchNowSources? {
        return if (episode == null) {
            val type = when (media.type) {
                MediaType.MOVIE -> "movies"
                MediaType.SHOW -> "shows"
            }

            cachedApiServiceForLinks.getStreams(
                id = media.id,
                type = type,
                country = countryCode
            )[countryCode]
        } else {
            cachedApiServiceForLinks.getEpisodeStreams(
                showId = media.id,
                season = episode.season,
                episode = episode.number,
                country = countryCode
            )[countryCode]
        }
    }

    /** Required to map which links to provide */
    private fun getCountry(context: Context): String {
        try {
            val response = cache30DayClient
                .request(TraktApiConfig.WATCHNOW_SOURCE_LOCATION_GETTER_URL)
                .execute()

            val country = response.body.string().trim().takeIf { it.length == 2 }
            if (country == null) {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val simCountry = tm.simCountryIso
                if (!simCountry.isNullOrBlank()) return simCountry.uppercase()

                val networkCountry = tm.networkCountryIso
                if (!networkCountry.isNullOrBlank()) return networkCountry.uppercase()

                // Fall back to locale
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.resources.configuration.locales[0].country
                } else {
                    @Suppress("DEPRECATION")
                    context.resources.configuration.locale.country
                }
            }

            return country
        } catch (e: Throwable) {
            errorLog("Failed to obtain country from ipinfo.io")
            e.printStackTrace()
            return DEFAULT_WATCHNOW_SOURCE
        }
    }
}