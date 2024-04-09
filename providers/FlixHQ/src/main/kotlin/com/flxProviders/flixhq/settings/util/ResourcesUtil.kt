package com.flxProviders.flixhq.settings.util

import android.content.res.Resources
import com.flixclusive.provider.Provider
import com.flxProviders.flixhq.BuildConfig


/**
 *
 * Based from [Cloudstream3](https://github.com/recloudstream/TestPlugins/blob/9674b68572c888a32f2e14be37f6c28e414a0f73/ExampleProvider/src/main/kotlin/com/example/BlankFragment.kt#L47)
 */
internal fun Resources.getString(name: String): String {
    val id = getIdentifier(name, "string", BuildConfig.LIBRARY_PACKAGE_NAME)
    return getString(id)
}