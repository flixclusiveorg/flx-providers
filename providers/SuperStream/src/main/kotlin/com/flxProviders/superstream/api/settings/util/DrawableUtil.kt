package com.flxProviders.superstream.api.settings.util

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.res.ResourcesCompat
import com.flxProviders.superstream.BuildConfig

internal object DrawableUtil {
    fun Resources.getDrawable(name: String): Drawable? {
        val id = getIdentifier(name, "drawable", BuildConfig.LIBRARY_PACKAGE_NAME)
        return ResourcesCompat.getDrawable(this, id, null)
    }

    fun Drawable.getBitmapFromImage(): ImageBitmap {
        // in below line we are creating our bitmap and initializing it.
        val bit = Bitmap.createBitmap(
            intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888
        )

        // on below line we are
        // creating a variable for canvas.
        val canvas = Canvas(bit)

        // on below line we are setting bounds for our bitmap.
        setBounds(0, 0, canvas.width, canvas.height)

        // on below line we are simply
        // calling draw to draw our canvas.
        draw(canvas)

        // on below line we are
        // returning our bitmap.
        return bit.asImageBitmap()
    }
}
