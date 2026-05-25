package com.flixclusive.provider.app.stremio.core.util

import com.flixclusive.core.util.coroutines.FlxDispatchers.Companion.withIOContext
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.model.provider.Catalog
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.provider.app.stremio.core.model.Addon
import com.flixclusive.provider.app.stremio.core.model.StremioCatalog
import com.flixclusive.provider.extensions.getString
import com.flixclusive.provider.extensions.remove
import com.flixclusive.provider.extensions.setString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

internal abstract class AddonAddResponse(
    val error: String? = null
)

internal data object Success : AddonAddResponse()
internal class Failed(error: String) : AddonAddResponse(error)
internal data object Duplicate: AddonAddResponse()

internal object AddonUtil {
    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }
    const val DEFAULT_META_PROVIDER_BASE_URL = "https://v3-cinemeta.strem.io"
    const val DEFAULT_META_PROVIDER_ID = "com.linvo.cinemeta"

    const val STREAMIO_ADDONS_KEY = "streamio_addons"
    const val ADDON_SOURCE_KEY = "addonSource"
    const val MEDIA_TYPE_KEY = "type"


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


    private suspend inline fun <reified T> DataStore<Preferences>.getObject(key: String, default: T?): T? {
        val raw = getString(key, null) ?: return default
        return runCatching { json.decodeFromString<T>(raw) }.getOrNull() ?: default
    }

    private suspend inline fun <reified T> DataStore<Preferences>.setObject(key: String, value: T?) {
        val encoded = value?.let { json.encodeToString(it) }
        if (encoded == null) remove(key) else setString(key, encoded)
    }

    suspend fun DataStore<Preferences>.getAddons(): List<Addon> {
        return getObject<List<Addon>>(STREAMIO_ADDONS_KEY, emptyList())!!
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

    suspend fun DataStore<Preferences>.addAddon(addon: Addon): AddonAddResponse {
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

    suspend fun DataStore<Preferences>.updateAddon(addon: Addon): AddonAddResponse {
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

    suspend fun DataStore<Preferences>.removeAddon(
        addon: Addon
    ): Boolean {
        safeCall {
            val addons = getAddons().toMutableList()
            addons.remove(addon)
            setObject(STREAMIO_ADDONS_KEY, addons.toList())
        } ?: return false

        return true
    }


    suspend fun DataStore<Preferences>.getAddon(id: String): Addon {
        return getAddons()
            .firstOrNull { it.id.equals(id, true) }
            ?: throw IllegalArgumentException("[${id}]> Addon cannot be found")
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
                        catalogs = catalogs
                    )
                }
        }
    }

    fun StremioCatalog.toCatalog(
        providerId: String,
        addonId: String,
        image: String? = null,
    ): Catalog {
        val json = copy(addonId = addonId)
            .toJson()

        return Catalog(
            name = name,
            url = json,
            image = image,
            providerId = providerId,
            canPaginate = canPaginate,
        )
    }

    private fun StremioCatalog.toJson(): String {
        return json.encodeToString(this)
    }
}