package com.flxProviders.superstream.api.util

import android.annotation.SuppressLint
import android.content.res.Resources
import com.flixclusive.provider.util.res.ProviderNoResourceFoundException
import com.flxProviders.superstream.BuildConfig
import java.io.InputStream

object ResourceUtil {
    @SuppressLint("DiscouragedApi")
    fun getRawFileInputStream(
        resources: Resources,
        fileName: String
    ): InputStream {
        val resourceId = resources.getIdentifier(fileName, "raw", BuildConfig.LIBRARY_PACKAGE_NAME)

        return if (resourceId != 0) {
            resources.openRawResource(resourceId)
        } else {
            throw ProviderNoResourceFoundException(
                name = fileName,
                type = "raw"
            )
        }
    }
}