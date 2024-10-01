package com.flxProviders.stremio.settings.util

import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.provider.settings.ProviderSettings
import com.flxProviders.stremio.api.STREAMIO_ADDONS_KEY
import com.flxProviders.stremio.api.STREMIO
import com.flxProviders.stremio.api.model.Addon
import com.flxProviders.stremio.api.model.Catalog
import com.google.gson.Gson
import okhttp3.OkHttpClient

internal abstract class AddonAddResponse(
    val error: String? = null
)

internal data object Success : AddonAddResponse()
internal class Failed(error: String) : AddonAddResponse(error)
internal data object Duplicate: AddonAddResponse()

internal object AddonUtil {
    private val gson = Gson()
    const val DEFAULT_META_PROVIDER_BASE_URL = "https://v3-cinemeta.strem.io"
    const val DEFAULT_META_PROVIDER = "Cinemeta"

    fun parseStremioAddonUrl(url: String): String {
        val regex = Regex("""(https?://.+)(?=/+)""")
        val basicUrlRegex = Regex("""(https?://.+)""")
        val streamioRegex = Regex("""stremio://(.+)(?=/+)""")

        val stremioRegexUrl = streamioRegex.find(url)?.groupValues?.get(1) ?: ""

        return when {
            regex.containsMatchIn(url) -> regex.find(url)?.groupValues?.get(1) ?: ""
            streamioRegex.containsMatchIn(url) -> "https://$stremioRegexUrl"
            basicUrlRegex.containsMatchIn(url) -> basicUrlRegex.find(url)?.groupValues?.get(1) ?: ""
            else -> ""
        }
    }


    fun ProviderSettings.getAddons(): List<Addon> {
        return getObject(STREAMIO_ADDONS_KEY) ?: emptyList()
    }
    
    suspend fun OkHttpClient.downloadAddon(url: String): Addon? {
        val addon = withIOContext {
            getManifest(addonUrl = url)
        }

        if (addon == null) {
            return null
        }

        return addon
    }

    fun ProviderSettings.addAddon(addon: Addon): AddonAddResponse {
        try {
            val addons = getAddons().toMutableList()
            if (addons.contains(addon))
                return Duplicate

            addons.add(addon)
            setObject(STREAMIO_ADDONS_KEY, addons.toList())

            return Success
        } catch (e: Exception) {
            errorLog(e)
            return Failed(e.actualMessage)
        }
    }

    fun ProviderSettings.updateAddon(addon: Addon): AddonAddResponse {
        try {
            val addons = getAddons().toMutableList()
            val index = addons.indexOfFirst { it == addon }

            if (index == -1) {
                return Failed("Addon [${addon.name}] not found")
            }

            addons[index] = addon
            setObject(STREAMIO_ADDONS_KEY, addons.toList())
            return Success
        } catch (e: Exception) {
            errorLog(e)
            return Failed(e.actualMessage)
        }
    }

    fun ProviderSettings.removeAddon(
        addon: Addon
    ): Boolean {
        safeCall {
            val addons = getAddons().toMutableList()
            addons.remove(addon)
            setObject(STREAMIO_ADDONS_KEY, addons.toList())
        } ?: return false

        return true
    }

    private fun OkHttpClient.getManifest(addonUrl: String): Addon? {
        return safeCall {
            request(url = "$addonUrl/manifest.json").execute()
                .fromJson<Addon>().run {
                    copy(
                        baseUrl = addonUrl,
                        logo = logo
                            ?.replace("svg", "png")
                            ?.replace("http:", "https:"),
                        catalogs = catalogs?.map {
                            it.copy(addonSource = name)
                        }
                    )
                }
        }
    }

    fun Catalog.toProviderCatalog(image: String? = null): ProviderCatalog {
        val json = toJson()
            ?: throw IllegalArgumentException("Invalid catalog")

        return ProviderCatalog(
            name = name,
            url = json,
            image = image,
            providerName = STREMIO,
            canPaginate = canPaginate
        )
    }

    private fun Catalog.toJson(): String? {
        return gson.toJson(this)
    }
}