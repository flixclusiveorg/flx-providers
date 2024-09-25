@file:Suppress("SpellCheckingInspection")

package com.flxProviders.stremio.settings

import android.util.TypedValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.ResourceResolutionException
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flixclusive.core.util.android.showToast
import com.flixclusive.provider.settings.ProviderSettings
import com.flixclusive.provider.util.res.LocalResources
import com.flixclusive.provider.util.res.ProviderNoResourceFoundException
import com.flixclusive.provider.util.res.painterResource
import com.flixclusive.provider.util.res.stringResource
import com.flxProviders.stremio.BuildConfig
import com.flxProviders.stremio.api.model.Addon
import com.flxProviders.stremio.settings.util.AddonUtil.downloadAddon
import com.flxProviders.stremio.settings.util.AddonUtil.getAddons
import com.flxProviders.stremio.settings.util.AddonUtil.removeAddon
import com.flxProviders.stremio.settings.util.AddonUtil.updateAddon
import com.flxProviders.stremio.settings.util.Failed
import com.flxProviders.stremio.settings.util.Success
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@Composable
internal fun StreamioScreen(
    settings: ProviderSettings,
    client: OkHttpClient
) {
    val context = LocalContext.current

    var reactiveInt by remember { mutableIntStateOf(0) } // Wtf is this?
    val addons = remember(settings.getAddons(), reactiveInt) {
        settings.getAddons()
    }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editAddon by remember { mutableStateOf<Addon?>(null) }

    val scope = rememberCoroutineScope()
    var updateJob by remember { mutableStateOf<Job?>(null) }

    fun updateAddon(addon: Addon) {
        if (updateJob?.isActive == true)
            return

        updateJob = scope.launch {
            val url = addon.baseUrl ?: return@launch
            val updatedAddon = client.downloadAddon(url = url)

            if (updatedAddon == null) {
                context.showToast("Failed to parse addon url [${addon.name}]")
                return@launch
            }

            val message = when (val response = settings.updateAddon(addon = updatedAddon)) {
                is Failed -> "Failed to add addon [${updatedAddon.name}]: ${response.error}"
                Success -> "Addon [${updatedAddon.name}] has been updated!"
                else -> null
            }

            if (message != null) {
                context.showToast(message)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AnimatedContent(
            targetState = addons.isEmpty(),
            label = ""
        ) { state ->
            when(state) {
                true -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No addons found ಠ⌣ಠ",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxSize()
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item(
                            key = null,
                            contentType = null
                        ) {
                            Text(
                                text = "Addons ヾ(⌐■_■)ノ♪",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                        }
                        items(
                            items = addons,
                            key = null,
                            contentType = { null }
                        ) {
                            AddonCard(
                                addon = it,
                                onEdit = { editAddon = it },
                                onUpdateManifest = { updateAddon(it) },
                                onRemove = {
                                    val success = settings.removeAddon(it)

                                    if (success) {
                                        reactiveInt++
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .fillMaxWidth()
                .height(70.dp)
        ) {
            ElevatedButton(
                onClick = { showAddDialog = true },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource("add", BuildConfig.LIBRARY_PACKAGE_NAME),
                        contentDescription = stringResource(
                            name = "add_addon_icon_content_desc",
                            packageName = BuildConfig.LIBRARY_PACKAGE_NAME
                        )
                    )

                    Text("Add stremio addon")
                }
            }
        }
    }

    if (showAddDialog || editAddon != null) {
        AddDialog(
            client = client,
            settings = settings,
            addonToEdit = editAddon,
            onDismiss = {
                showAddDialog = false
                editAddon = null
            }
        )
    }
}

@Composable
fun spainterResource(name: String, packageName: String): Painter {
    val res = LocalResources.current
    val context = LocalContext.current

    val id = res.getIdentifier(name, "drawable", packageName)
    if (id == 0) {
        throw ProviderNoResourceFoundException(
            name = name,
            type = "drawable"
        )
    }

    val value = remember { TypedValue() }
    res.getValue(id, value, true)
    val path = value.string

    return if (path?.endsWith(".xml") == true) {
        val imageVector = remember(id, res, res.configuration) {
            ImageVector.vectorResource(null, res, id)
        }

        rememberVectorPainter(imageVector)
    } else {
        // Otherwise load the bitmap resource
        val imageBitmap = remember(path, id, context.theme) {
            try {
                ImageBitmap.imageResource(res, id)
            } catch (exception: Exception) {
                throw ResourceResolutionException("Error attempting to load resource: $path", exception)
            }
        }

        BitmapPainter(imageBitmap)
    }
}