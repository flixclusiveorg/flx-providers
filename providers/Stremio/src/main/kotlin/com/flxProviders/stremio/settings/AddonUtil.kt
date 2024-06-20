package com.flxProviders.stremio.settings

import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.provider.settings.ProviderSettingsManager
import com.flxProviders.stremio.api.STREAMIO_ADDONS_KEY
import com.flxProviders.stremio.api.dto.StremioAddOn
import okhttp3.OkHttpClient

internal object AddonUtil {
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


    fun ProviderSettingsManager.getAddons(): List<StremioAddOn> {
        return getObject(STREAMIO_ADDONS_KEY) ?: emptyList()
    }

    fun ProviderSettingsManager.addAddon(
        addon: StremioAddOn
    ): Boolean {
        safeCall {
            val addons = getAddons().toMutableList()
            addons.add(addon)
            setObject(STREAMIO_ADDONS_KEY, addons.toList())
        } ?: return false


        return true
    }

    fun ProviderSettingsManager.removeAddon(
        addon: StremioAddOn
    ): Boolean {
        safeCall {
            val addons = getAddons().toMutableList()
            addons.remove(addon)
            setObject(STREAMIO_ADDONS_KEY, addons.toList())
        } ?: return false

        return true
    }

    fun OkHttpClient.getManifest(addonUrl: String): StremioAddOn? {
        return safeCall {
            request(url = "$addonUrl/manifest.json").execute()
                .fromJson<StremioAddOn>().copy(
                    baseUrl = addonUrl
                )
        }
    }
}